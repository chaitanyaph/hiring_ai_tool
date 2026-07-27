package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import com.cadence.codingassessmentservice.constants.TestCaseVisibility;
import com.cadence.codingassessmentservice.dto.request.RunCodeRequest;
import com.cadence.codingassessmentservice.dto.request.SubmitCodeRequest;
import com.cadence.codingassessmentservice.dto.response.RunCodeResponse;
import com.cadence.codingassessmentservice.dto.response.SubmitCodeResponse;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.entity.ExecutionLog;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.QuestionTestCase;
import com.cadence.codingassessmentservice.entity.Submission;
import com.cadence.codingassessmentservice.entity.SubmissionTestCase;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.execution.ExecutionRequest;
import com.cadence.codingassessmentservice.execution.ExecutionResult;
import com.cadence.codingassessmentservice.kafka.event.CodeSubmittedEvent;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.repository.ExecutionLogRepository;
import com.cadence.codingassessmentservice.repository.QuestionRepository;
import com.cadence.codingassessmentservice.repository.QuestionTestCaseRepository;
import com.cadence.codingassessmentservice.repository.SubmissionRepository;
import com.cadence.codingassessmentservice.repository.SubmissionTestCaseRepository;
import com.cadence.codingassessmentservice.service.CodeExecutionService;
import com.cadence.codingassessmentservice.strategy.CodeExecutionProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionServiceImpl implements CodeExecutionService {

    /** Worst-to-best so the overall submission verdict reflects the most severe outcome seen across every test case. */
    private static final List<SubmissionStatus> STATUS_SEVERITY = List.of(
            SubmissionStatus.COMPILE_ERROR, SubmissionStatus.RUNTIME_ERROR, SubmissionStatus.TIME_LIMIT_EXCEEDED,
            SubmissionStatus.MEMORY_LIMIT_EXCEEDED, SubmissionStatus.WRONG_ANSWER, SubmissionStatus.ACCEPTED);

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionTestCaseRepository submissionTestCaseRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final CodeExecutionProviderFactory providerFactory;
    private final CodingAssessmentEventProducer eventProducer;

    @Value("${coding-assessment.execution.default-time-limit-seconds:5}")
    private int defaultTimeLimitSeconds;

    @Value("${coding-assessment.execution.default-memory-limit-kb:131072}")
    private int defaultMemoryLimitKb;

    @Override
    @Transactional
    public RunCodeResponse runCode(RunCodeRequest request) {
        CandidateAssessment ca = findInProgressOrThrow(request.getCandidateAssessmentId());
        Question question = findQuestionOrThrow(request.getQuestionId());

        String stdin = request.getCustomInput();
        if (stdin == null) {
            List<QuestionTestCase> visible = testCaseRepository.findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(question.getId(), TestCaseVisibility.VISIBLE);
            stdin = visible.isEmpty() ? "" : visible.get(0).getInputData();
        }

        ExecutionResult result = providerFactory.getActiveProvider().execute(new ExecutionRequest(
                request.getLanguage(), request.getCode(), stdin, null, defaultTimeLimitSeconds, defaultMemoryLimitKb));

        executionLogRepository.save(ExecutionLog.builder()
                .candidateAssessmentId(ca.getId()).questionId(question.getId())
                .language(request.getLanguage()).code(request.getCode()).customInput(request.getCustomInput())
                .output(result.stdout()).stderr(result.stderr())
                .runtimeMs(result.runtimeMs()).memoryKb(result.memoryKb())
                .build());

        return RunCodeResponse.builder()
                .output(result.stdout()).stderr(result.stderr()).compileOutput(result.compileOutput())
                .runtimeMs(result.runtimeMs()).memoryKb(result.memoryKb())
                .build();
    }

    @Override
    @Transactional
    public SubmitCodeResponse submitCode(SubmitCodeRequest request) {
        CandidateAssessment ca = findInProgressOrThrow(request.getCandidateAssessmentId());
        Question question = findQuestionOrThrow(request.getQuestionId());
        List<QuestionTestCase> testCases = testCaseRepository.findAllByQuestionIdOrderByDisplayOrderAsc(question.getId());
        if (testCases.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "This question has no test cases configured");
        }

        int attemptNumber = (int) submissionRepository.countByCandidateAssessmentIdAndQuestionId(ca.getId(), question.getId()) + 1;

        List<SubmitCodeResponse.TestCaseResult> caseResults = new ArrayList<>();
        List<SubmissionTestCase> persistedCases = new ArrayList<>();
        SubmissionStatus overallStatus = SubmissionStatus.ACCEPTED;
        int passed = 0;
        Integer maxRuntimeMs = null;
        Integer maxMemoryKb = null;
        String firstCompileOutput = null;
        int order = 0;

        for (QuestionTestCase testCase : testCases) {
            ExecutionResult result = providerFactory.getActiveProvider().execute(new ExecutionRequest(
                    request.getLanguage(), request.getCode(), testCase.getInputData(), testCase.getExpectedOutput(),
                    defaultTimeLimitSeconds, defaultMemoryLimitKb));

            boolean casePassed = result.status() == SubmissionStatus.ACCEPTED;
            if (casePassed) {
                passed++;
            }
            overallStatus = worseOf(overallStatus, result.status());
            maxRuntimeMs = maxOrNull(maxRuntimeMs, result.runtimeMs());
            maxMemoryKb = maxOrNull(maxMemoryKb, result.memoryKb());
            if (firstCompileOutput == null && result.compileOutput() != null && !result.compileOutput().isBlank()) {
                firstCompileOutput = result.compileOutput();
            }

            boolean visible = testCase.getVisibility() == TestCaseVisibility.VISIBLE;
            persistedCases.add(SubmissionTestCase.builder()
                    .testCaseId(testCase.getId()).passed(casePassed).actualOutput(result.stdout())
                    .runtimeMs(result.runtimeMs()).memoryKb(result.memoryKb()).displayOrder(order).build());

            caseResults.add(SubmitCodeResponse.TestCaseResult.builder()
                    .visible(visible).passed(casePassed)
                    .inputData(visible ? testCase.getInputData() : null)
                    .expectedOutput(visible ? testCase.getExpectedOutput() : null)
                    .actualOutput(visible ? result.stdout() : null)
                    .build());
            order++;

            // A compile error is identical for every test case -- no point burning further Judge0 calls.
            if (result.status() == SubmissionStatus.COMPILE_ERROR) {
                break;
            }
        }

        int totalConsidered = persistedCases.size();
        int score = totalConsidered == 0 ? 0 : Math.round((float) question.getMarks() * passed / totalConsidered);

        Submission submission = Submission.builder()
                .candidateAssessmentId(ca.getId()).questionId(question.getId())
                .language(request.getLanguage()).code(request.getCode())
                .status(overallStatus).score(score)
                .testCasesPassed(passed).testCasesTotal(totalConsidered)
                .runtimeMs(maxRuntimeMs).memoryKb(maxMemoryKb)
                .attemptNumber(attemptNumber).compileOutput(firstCompileOutput)
                .submittedAt(LocalDateTime.now())
                .build();
        submission = submissionRepository.save(submission);

        for (SubmissionTestCase stc : persistedCases) {
            stc.setSubmissionId(submission.getId());
        }
        submissionTestCaseRepository.saveAll(persistedCases);

        eventProducer.publishCodeSubmitted(CodeSubmittedEvent.builder()
                .submissionId(submission.getId()).candidateAssessmentId(ca.getId()).questionId(question.getId())
                .applicationId(ca.getApplicationId()).occurredAt(LocalDateTime.now()).build());

        return SubmitCodeResponse.builder()
                .submissionId(submission.getId()).status(overallStatus).score(score)
                .testCasesPassed(passed).testCasesTotal(totalConsidered)
                .runtimeMs(maxRuntimeMs).memoryKb(maxMemoryKb).compileOutput(firstCompileOutput)
                .testCaseResults(caseResults)
                .build();
    }

    private SubmissionStatus worseOf(SubmissionStatus a, SubmissionStatus b) {
        return STATUS_SEVERITY.indexOf(a) <= STATUS_SEVERITY.indexOf(b) ? a : b;
    }

    private Integer maxOrNull(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }

    private CandidateAssessment findInProgressOrThrow(UUID candidateAssessmentId) {
        CandidateAssessment ca = candidateAssessmentRepository.findById(candidateAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No assessment attempt found: " + candidateAssessmentId));
        if (ca.getStatus() != CandidateAssessmentStatus.IN_PROGRESS) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "This assessment is not currently in progress");
        }
        return ca;
    }

    private Question findQuestionOrThrow(UUID questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId));
    }
}
