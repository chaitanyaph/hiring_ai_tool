package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.entity.AssessmentQuestion;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.repository.*;
import com.cadence.codingassessmentservice.service.CodingEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateAssessmentServiceImplTest {

    @Mock private CandidateAssessmentRepository candidateAssessmentRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private CandidateQuestionProgressRepository progressRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionStarterCodeRepository starterCodeRepository;
    @Mock private QuestionTestCaseRepository testCaseRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private AntiCheatLogRepository antiCheatLogRepository;
    @Mock private CodingAssessmentEventProducer eventProducer;
    @Mock private CodingEvaluationService codingEvaluationService;

    @InjectMocks
    private CandidateAssessmentServiceImpl candidateAssessmentService;

    private UUID candidateAssessmentId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(candidateAssessmentService, "ttlHours", 168);
        candidateAssessmentId = UUID.randomUUID();
        lenient().when(candidateAssessmentRepository.save(any(CandidateAssessment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void acceptRules_shouldThrow_whenAlreadyStarted() {
        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.IN_PROGRESS).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        assertThatThrownBy(() -> candidateAssessmentService.acceptRules(candidateAssessmentId))
                .isInstanceOf(AssessmentConflictException.class);
    }

    @Test
    void startAssessment_shouldThrow_whenRulesNotAccepted() {
        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.NOT_STARTED)
                .expiresAt(LocalDateTime.now().plusDays(1)).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        assertThatThrownBy(() -> candidateAssessmentService.startAssessment(candidateAssessmentId, ProgrammingLanguage.JAVA))
                .isInstanceOf(AssessmentConflictException.class);
    }

    @Test
    void startAssessment_shouldTransitionAndReturnFirstQuestion_whenRulesAccepted() {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        CandidateAssessment ca = CandidateAssessment.builder().assessmentId(assessmentId)
                .status(CandidateAssessmentStatus.NOT_STARTED).expiresAt(LocalDateTime.now().plusDays(1))
                .rulesAcceptedAt(LocalDateTime.now()).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        AssessmentQuestion link = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).displayOrder(0).build();
        when(assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(assessmentId)).thenReturn(List.of(link));

        Question question = Question.builder().title("Two Sum").difficulty(Difficulty.EASY).marks(10)
                .description("desc").allowedLanguages("JAVA").build();
        ReflectionTestUtils.setField(question, "id", questionId);
        when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(progressRepository.findByCandidateAssessmentIdAndQuestionId(candidateAssessmentId, questionId)).thenReturn(Optional.empty());
        when(testCaseRepository.findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(any(), any())).thenReturn(List.of());
        when(submissionRepository.findFirstByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(any(), any())).thenReturn(Optional.empty());

        var response = candidateAssessmentService.startAssessment(candidateAssessmentId, ProgrammingLanguage.JAVA);

        assertThat(ca.getStatus()).isEqualTo(CandidateAssessmentStatus.IN_PROGRESS);
        assertThat(response.getTitle()).isEqualTo("Two Sum");
        assertThat(response.getNavigatorStatus()).isEqualTo("VISITED");
        verify(eventProducer).publishAssessmentStarted(any());
    }

    @Test
    void finishAssessment_shouldComputeTimeUsedAndTriggerEvaluation() {
        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusMinutes(30)).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        candidateAssessmentService.finishAssessment(candidateAssessmentId, false);

        assertThat(ca.getStatus()).isEqualTo(CandidateAssessmentStatus.COMPLETED);
        assertThat(ca.getTimeUsedSeconds()).isGreaterThanOrEqualTo(1799);
        assertThat(ca.isAutoSubmitted()).isFalse();
        verify(codingEvaluationService).evaluate(candidateAssessmentId);
    }

    @Test
    void finishAssessment_shouldThrow_whenNotInProgress() {
        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.NOT_STARTED).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        assertThatThrownBy(() -> candidateAssessmentService.finishAssessment(candidateAssessmentId, false))
                .isInstanceOf(AssessmentConflictException.class);
        verifyNoInteractions(codingEvaluationService);
    }

    @Test
    void recordAntiCheatEvent_shouldPersistLog() {
        CandidateAssessment ca = CandidateAssessment.builder().status(CandidateAssessmentStatus.IN_PROGRESS).build();
        ReflectionTestUtils.setField(ca, "id", candidateAssessmentId);
        when(candidateAssessmentRepository.findById(candidateAssessmentId)).thenReturn(Optional.of(ca));

        candidateAssessmentService.recordAntiCheatEvent(candidateAssessmentId, AntiCheatEventType.TAB_SWITCH, null);

        verify(antiCheatLogRepository).save(argThat(log -> log.getEventType() == AntiCheatEventType.TAB_SWITCH
                && log.getCandidateAssessmentId().equals(candidateAssessmentId)));
    }
}
