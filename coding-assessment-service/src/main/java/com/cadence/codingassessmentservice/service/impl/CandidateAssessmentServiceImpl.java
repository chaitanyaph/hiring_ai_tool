package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.*;
import com.cadence.codingassessmentservice.dto.response.IdeQuestionResponse;
import com.cadence.codingassessmentservice.entity.*;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.kafka.event.AssessmentStartedEvent;
import com.cadence.codingassessmentservice.kafka.event.CodingAssessmentInvitedEvent;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.repository.*;
import com.cadence.codingassessmentservice.service.CandidateAssessmentService;
import com.cadence.codingassessmentservice.service.CodingEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestration for a candidate's own attempt. Mirrors ai-interview-
 * service's InterviewSessionServiceImpl split: this is the synchronous
 * front door for every candidate action; the heavier post-completion
 * evaluation is deliberately async (CodingEvaluationService), fired
 * and not awaited from finishAssessment().
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateAssessmentServiceImpl implements CandidateAssessmentService {

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final CandidateQuestionProgressRepository progressRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionStarterCodeRepository starterCodeRepository;
    private final QuestionTestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final AntiCheatLogRepository antiCheatLogRepository;
    private final CodingAssessmentEventProducer eventProducer;
    private final CodingEvaluationService codingEvaluationService;

    @Value("${coding-assessment.session.default-invitation-ttl-hours:168}")
    private int ttlHours;

    @Value("${cadence.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void inviteCandidate(UUID assessmentId, UUID applicationId, UUID jobId, UUID candidateId) {
        CandidateAssessment ca = candidateAssessmentRepository.findByAssessmentIdAndApplicationId(assessmentId, applicationId)
                .orElseGet(() -> CandidateAssessment.builder()
                        .assessmentId(assessmentId).applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                        .status(CandidateAssessmentStatus.NOT_STARTED)
                        .build());
        ca.setInvitedAt(LocalDateTime.now());
        ca.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        if (ca.getStatus() == CandidateAssessmentStatus.EXPIRED) {
            ca.setStatus(CandidateAssessmentStatus.NOT_STARTED);
        }
        ca = candidateAssessmentRepository.save(ca);
        seedQuestionProgress(ca.getId(), assessmentId);

        Assessment assessment = assessmentRepository.findById(assessmentId).orElse(null);
        eventProducer.publishCodingAssessmentInvited(CodingAssessmentInvitedEvent.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .assessmentName(assessment != null ? assessment.getName() : null)
                .durationMinutes(assessment != null ? assessment.getDurationMinutes() : null)
                .passingScorePercent(assessment != null ? assessment.getPassingScorePercent() : null)
                .assessmentLink(frontendBaseUrl + "/candidate/coding-assessments/" + ca.getId())
                .expiresAt(ca.getExpiresAt())
                .occurredAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional
    public void sendReminder(UUID candidateAssessmentId) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        if (ca.getStatus() != CandidateAssessmentStatus.NOT_STARTED) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "Only a not-started assessment can be reminded");
        }
        // Notification Service does not exist yet in this platform -- this
        // is a validated no-op today, logged for audit/traceability.
        ca.setRemindedAt(LocalDateTime.now());
        candidateAssessmentRepository.save(ca);
    }

    @Override
    @Transactional
    public void resendInvite(UUID candidateAssessmentId) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        if (ca.getStatus() != CandidateAssessmentStatus.EXPIRED) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "Only an expired assessment can be resent");
        }
        ca.setStatus(CandidateAssessmentStatus.NOT_STARTED);
        ca.setInvitedAt(LocalDateTime.now());
        ca.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        candidateAssessmentRepository.save(ca);
    }

    @Override
    @Transactional
    public void acceptRules(UUID candidateAssessmentId) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        expireIfPastDeadline(ca);
        if (ca.getStatus() != CandidateAssessmentStatus.NOT_STARTED) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_ALREADY_STARTED, "This assessment has already been started or completed");
        }
        ca.setRulesAcceptedAt(LocalDateTime.now());
        candidateAssessmentRepository.save(ca);
    }

    @Override
    @Transactional
    public IdeQuestionResponse startAssessment(UUID candidateAssessmentId, ProgrammingLanguage preferredLanguage) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        expireIfPastDeadline(ca);
        if (ca.getStatus() != CandidateAssessmentStatus.NOT_STARTED) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_ALREADY_STARTED, "This assessment has already been started or completed");
        }
        if (ca.getRulesAcceptedAt() == null) {
            throw new AssessmentConflictException(ErrorCode.RULES_NOT_ACCEPTED, "You must accept the assessment rules before starting");
        }

        ca.setStatus(CandidateAssessmentStatus.IN_PROGRESS);
        ca.setStartedAt(LocalDateTime.now());
        ca.setCurrentQuestionIndex(0);
        ca.setLanguagePreference(preferredLanguage);
        candidateAssessmentRepository.save(ca);

        eventProducer.publishAssessmentStarted(AssessmentStartedEvent.builder()
                .candidateAssessmentId(ca.getId()).assessmentId(ca.getAssessmentId()).applicationId(ca.getApplicationId())
                .occurredAt(LocalDateTime.now()).build());

        return goToQuestion(candidateAssessmentId, 0);
    }

    @Override
    @Transactional
    public IdeQuestionResponse goToQuestion(UUID candidateAssessmentId, int questionIndex) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        if (ca.getStatus() != CandidateAssessmentStatus.IN_PROGRESS) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "This assessment is not currently in progress");
        }

        List<AssessmentQuestion> links = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(ca.getAssessmentId());
        if (questionIndex < 0 || questionIndex >= links.size()) {
            throw new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "No question at index " + questionIndex);
        }
        UUID questionId = links.get(questionIndex).getQuestionId();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found: " + questionId));

        CandidateQuestionProgress progress = progressRepository.findByCandidateAssessmentIdAndQuestionId(ca.getId(), questionId)
                .orElseGet(() -> CandidateQuestionProgress.builder()
                        .candidateAssessmentId(ca.getId()).questionId(questionId).displayOrder(questionIndex).build());
        progress.setVisited(true);
        progress.setLastVisitedAt(LocalDateTime.now());
        progressRepository.save(progress);

        ca.setCurrentQuestionIndex(questionIndex);
        candidateAssessmentRepository.save(ca);

        return buildIdeQuestionResponse(ca, question, questionIndex, links.size(), progress);
    }

    @Override
    @Transactional
    public void markForReview(UUID candidateAssessmentId, UUID questionId, boolean marked) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        if (ca.getStatus() != CandidateAssessmentStatus.IN_PROGRESS) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "This assessment is not currently in progress");
        }
        CandidateQuestionProgress progress = progressRepository.findByCandidateAssessmentIdAndQuestionId(candidateAssessmentId, questionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found for this assessment"));
        progress.setMarkedForReview(marked);
        progressRepository.save(progress);
    }

    @Override
    @Transactional
    public void finishAssessment(UUID candidateAssessmentId, boolean autoSubmitted) {
        CandidateAssessment ca = findByIdOrThrow(candidateAssessmentId);
        if (ca.getStatus() != CandidateAssessmentStatus.IN_PROGRESS) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "This assessment is not currently in progress");
        }

        ca.setStatus(CandidateAssessmentStatus.COMPLETED);
        ca.setCompletedAt(LocalDateTime.now());
        ca.setAutoSubmitted(autoSubmitted);
        if (ca.getStartedAt() != null) {
            ca.setTimeUsedSeconds((int) Duration.between(ca.getStartedAt(), ca.getCompletedAt()).getSeconds());
        }
        candidateAssessmentRepository.save(ca);

        codingEvaluationService.evaluate(ca.getId());
    }

    @Override
    @Transactional
    public void recordAntiCheatEvent(UUID candidateAssessmentId, AntiCheatEventType eventType, String metadata) {
        findByIdOrThrow(candidateAssessmentId);
        antiCheatLogRepository.save(AntiCheatLog.builder()
                .candidateAssessmentId(candidateAssessmentId).eventType(eventType).metadata(metadata).build());
    }

    private void seedQuestionProgress(UUID candidateAssessmentId, UUID assessmentId) {
        List<AssessmentQuestion> links = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(assessmentId);
        List<CandidateQuestionProgress> rows = new java.util.ArrayList<>();
        for (AssessmentQuestion link : links) {
            if (progressRepository.findByCandidateAssessmentIdAndQuestionId(candidateAssessmentId, link.getQuestionId()).isEmpty()) {
                rows.add(CandidateQuestionProgress.builder()
                        .candidateAssessmentId(candidateAssessmentId).questionId(link.getQuestionId())
                        .displayOrder(link.getDisplayOrder()).build());
            }
        }
        progressRepository.saveAll(rows);
    }

    private IdeQuestionResponse buildIdeQuestionResponse(CandidateAssessment ca, Question question, int questionIndex,
                                                           int totalQuestions, CandidateQuestionProgress progress) {
        ProgrammingLanguage language = ca.getLanguagePreference();
        String starterCode = language == null ? null
                : starterCodeRepository.findByQuestionIdAndLanguage(question.getId(), language)
                        .map(QuestionStarterCode::getCode).orElse(null);

        List<QuestionTestCase> visible = testCaseRepository.findAllByQuestionIdAndVisibilityOrderByDisplayOrderAsc(
                question.getId(), TestCaseVisibility.VISIBLE);
        List<IdeQuestionResponse.VisibleTestCase> visibleTestCases = visible.stream()
                .map(tc -> IdeQuestionResponse.VisibleTestCase.builder().inputData(tc.getInputData()).expectedOutput(tc.getExpectedOutput()).build())
                .toList();

        long hiddenCount = testCaseRepository.countByQuestionIdAndVisibility(question.getId(), TestCaseVisibility.HIDDEN);

        Optional<Submission> latest = submissionRepository.findFirstByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(ca.getId(), question.getId());
        String navigatorStatus = latest.filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED).isPresent()
                ? "COMPLETED" : (progress.isVisited() ? "VISITED" : "NOT_VISITED");

        return IdeQuestionResponse.builder()
                .questionId(question.getId())
                .questionOrder(questionIndex + 1)
                .totalQuestions(totalQuestions)
                .difficulty(question.getDifficulty())
                .marks(question.getMarks())
                .hiddenTestCaseCount((int) hiddenCount)
                .title(question.getTitle())
                .description(question.getDescription())
                .exampleText(question.getExampleText())
                .constraintsText(question.getConstraintsText())
                .allowedLanguages(splitCsv(question.getAllowedLanguages()))
                .starterCode(starterCode)
                .visibleTestCases(visibleTestCases)
                .navigatorStatus(navigatorStatus)
                .markedForReview(progress.isMarkedForReview())
                .build();
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).toList();
    }

    private void expireIfPastDeadline(CandidateAssessment ca) {
        if (ca.getStatus() == CandidateAssessmentStatus.NOT_STARTED
                && ca.getExpiresAt() != null && ca.getExpiresAt().isBefore(LocalDateTime.now())) {
            ca.setStatus(CandidateAssessmentStatus.EXPIRED);
            candidateAssessmentRepository.save(ca);
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_EXPIRED, "This assessment invitation has expired");
        }
    }

    private CandidateAssessment findByIdOrThrow(UUID candidateAssessmentId) {
        return candidateAssessmentRepository.findById(candidateAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No assessment attempt found: " + candidateAssessmentId));
    }
}
