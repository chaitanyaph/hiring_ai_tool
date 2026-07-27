package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.*;
import com.cadence.codingassessmentservice.dto.request.RunCodeRequest;
import com.cadence.codingassessmentservice.dto.request.SubmitCodeRequest;
import com.cadence.codingassessmentservice.dto.response.SubmitCodeResponse;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import com.cadence.codingassessmentservice.entity.Submission;
import com.cadence.codingassessmentservice.execution.CodeExecutionProvider;
import com.cadence.codingassessmentservice.execution.ExecutionRequest;
import com.cadence.codingassessmentservice.execution.ExecutionResult;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.repository.*;
import com.cadence.codingassessmentservice.strategy.CodeExecutionProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeExecutionServiceImplTest {

    @Mock private CandidateAssessmentRepository candidateAssessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionTestCaseRepository testCaseRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private SubmissionTestCaseRepository submissionTestCaseRepository;
    @Mock private ExecutionLogRepository executionLogRepository;
    @Mock private CodeExecutionProviderFactory providerFactory;
    @Mock private CodingAssessmentEventProducer eventProducer;
    @Mock private CodeExecutionProvider provider;

    @InjectMocks
    private CodeExecutionServiceImpl codeExecutionService;

    private UUID candidateAssessmentId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(codeExecutionService, "defaultTimeLimitSeconds", 5);
        ReflectionTestUtils.setField(codeExecutionService, "defaultMemoryLimitKb", 131072);
        candidateAssessmentId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.IN_PROGRESS).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        lenient().when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        Question question = Question.builder().title("Two Sum").difficulty(Difficulty.EASY).marks(100)
                .description("desc").allowedLanguages("JAVA").build();
        ReflectionTestUtils.setField(question, "id", questionId);
        lenient().when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        lenient().when(providerFactory.getActiveProvider()).thenReturn(provider);
        lenient().when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void submitCode_shouldBeFullyAccepted_whenEveryTestCasePasses() {
        QuestionTestCase tc1 = testCase(TestCaseVisibility.VISIBLE, "1 2", "3");
        QuestionTestCase tc2 = testCase(TestCaseVisibility.HIDDEN, "5 5", "10");
        when(testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId)).thenReturn(List.of(tc1, tc2));
        when(provider.execute(any())).thenReturn(new ExecutionResult(SubmissionStatus.ACCEPTED, "3", null, null, 5, 100));

        SubmitCodeResponse response = codeExecutionService.submitCode(submitRequest());

        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(response.getScore()).isEqualTo(100);
        assertThat(response.getTestCasesPassed()).isEqualTo(2);
        assertThat(response.getTestCaseResults()).hasSize(2);
        assertThat(response.getTestCaseResults().get(1).isVisible()).isFalse();
        assertThat(response.getTestCaseResults().get(1).getInputData()).isNull(); // hidden case never exposes input/expected/actual
        verify(eventProducer).publishCodeSubmitted(any());
    }

    @Test
    void submitCode_shouldComputeProportionalScore_whenSomeTestCasesFail() {
        QuestionTestCase tc1 = testCase(TestCaseVisibility.VISIBLE, "1 2", "3");
        QuestionTestCase tc2 = testCase(TestCaseVisibility.VISIBLE, "5 5", "10");
        when(testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId)).thenReturn(List.of(tc1, tc2));
        when(provider.execute(any()))
                .thenReturn(new ExecutionResult(SubmissionStatus.ACCEPTED, "3", null, null, 5, 100))
                .thenReturn(new ExecutionResult(SubmissionStatus.WRONG_ANSWER, "9", null, null, 5, 100));

        SubmitCodeResponse response = codeExecutionService.submitCode(submitRequest());

        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.WRONG_ANSWER);
        assertThat(response.getScore()).isEqualTo(50);
        assertThat(response.getTestCasesPassed()).isEqualTo(1);
    }

    @Test
    void submitCode_shouldStopEarly_onCompileError() {
        QuestionTestCase tc1 = testCase(TestCaseVisibility.VISIBLE, "1 2", "3");
        QuestionTestCase tc2 = testCase(TestCaseVisibility.VISIBLE, "5 5", "10");
        when(testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(questionId)).thenReturn(List.of(tc1, tc2));
        when(provider.execute(any())).thenReturn(new ExecutionResult(SubmissionStatus.COMPILE_ERROR, null, "syntax error", "syntax error", null, null));

        SubmitCodeResponse response = codeExecutionService.submitCode(submitRequest());

        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(response.getScore()).isEqualTo(0);
        assertThat(response.getTestCaseResults()).hasSize(1); // only the first case was attempted
        verify(provider, times(1)).execute(any());
    }

    @Test
    void submitCode_shouldThrow_whenAssessmentNotInProgress() {
        CandidateAssessment notStarted = CandidateAssessment.builder().status(CandidateAssessmentStatus.NOT_STARTED).build();
        UUID otherId = UUID.randomUUID();
        ReflectionTestUtils.setField(notStarted, "id", otherId);
        when(candidateAssessmentRepository.findById(otherId)).thenReturn(Optional.of(notStarted));

        SubmitCodeRequest request = SubmitCodeRequest.builder()
                .candidateAssessmentId(otherId).questionId(questionId).language(ProgrammingLanguage.JAVA).code("code").build();

        assertThatThrownBy(() -> codeExecutionService.submitCode(request)).isInstanceOf(AssessmentConflictException.class);
    }

    @Test
    void runCode_shouldPersistExecutionLogAndReturnOutput_withoutScoring() {
        when(testCaseRepository.findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(questionId, TestCaseVisibility.VISIBLE))
                .thenReturn(List.of(testCase(TestCaseVisibility.VISIBLE, "1 2", "3")));
        when(provider.execute(any())).thenReturn(new ExecutionResult(SubmissionStatus.ACCEPTED, "3", null, null, 5, 100));

        RunCodeRequest request = RunCodeRequest.builder()
                .candidateAssessmentId(candidateAssessmentId).questionId(questionId)
                .language(ProgrammingLanguage.JAVA).code("code").build();

        var response = codeExecutionService.runCode(request);

        assertThat(response.getOutput()).isEqualTo("3");
        verify(executionLogRepository).save(any());
        verifyNoInteractions(submissionRepository);
    }

    private QuestionTestCase testCase(TestCaseVisibility visibility, String input, String expected) {
        return QuestionTestCase.builder().questionId(questionId).visibility(visibility)
                .inputData(input).expectedOutput(expected).build();
    }

    private SubmitCodeRequest submitRequest() {
        return SubmitCodeRequest.builder()
                .candidateAssessmentId(candidateAssessmentId).questionId(questionId)
                .language(ProgrammingLanguage.JAVA).code("public class Main {}").build();
    }
}
