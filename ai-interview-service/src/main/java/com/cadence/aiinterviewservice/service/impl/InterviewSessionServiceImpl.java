package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.*;
import com.cadence.aiinterviewservice.dto.request.AnswerRequest;
import com.cadence.aiinterviewservice.dto.response.InterviewQuestionResponse;
import com.cadence.aiinterviewservice.entity.InterviewAnswer;
import com.cadence.aiinterviewservice.entity.InterviewLog;
import com.cadence.aiinterviewservice.entity.InterviewQuestion;
import com.cadence.aiinterviewservice.entity.InterviewRecommendation;
import com.cadence.aiinterviewservice.entity.InterviewSession;
import com.cadence.aiinterviewservice.exception.ErrorCode;
import com.cadence.aiinterviewservice.exception.InterviewConflictException;
import com.cadence.aiinterviewservice.exception.ResourceNotFoundException;
import com.cadence.aiinterviewservice.feign.JobServiceClient;
import com.cadence.aiinterviewservice.feign.ResumeParserServiceClient;
import com.cadence.aiinterviewservice.feign.dto.JobDetailDto;
import com.cadence.aiinterviewservice.feign.dto.JobSkillDto;
import com.cadence.aiinterviewservice.feign.dto.MatchedSkillDto;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import com.cadence.aiinterviewservice.kafka.event.AiInterviewInvitedEvent;
import com.cadence.aiinterviewservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.aiinterviewservice.kafka.event.InterviewCompletedEvent;
import com.cadence.aiinterviewservice.kafka.event.InterviewStartedEvent;
import com.cadence.aiinterviewservice.kafka.producer.AiInterviewEventProducer;
import com.cadence.aiinterviewservice.provider.CandidateResumeSnapshot;
import com.cadence.aiinterviewservice.provider.GeneratedQuestion;
import com.cadence.aiinterviewservice.provider.InterviewQuestionContext;
import com.cadence.aiinterviewservice.provider.JobContextSnapshot;
import com.cadence.aiinterviewservice.provider.QaPair;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.repository.CandidateShortlistRepository;
import com.cadence.aiinterviewservice.repository.InterviewAnswerRepository;
import com.cadence.aiinterviewservice.repository.InterviewLogRepository;
import com.cadence.aiinterviewservice.repository.InterviewQuestionRepository;
import com.cadence.aiinterviewservice.repository.InterviewRecommendationRepository;
import com.cadence.aiinterviewservice.repository.InterviewSessionRepository;
import com.cadence.aiinterviewservice.service.InterviewEvaluationService;
import com.cadence.aiinterviewservice.service.InterviewSessionService;
import com.cadence.aiinterviewservice.strategy.AIInterviewProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestration for the AI Interview module. Question generation is
 * synchronous (the candidate is actively waiting for the next
 * question in the same request), unlike the heavier post-completion
 * evaluation, which is deliberately async -- the completion screen in
 * the Figma literally says "you'll see your status update within a
 * few hours," so InterviewEvaluationService.evaluate() is fired and
 * NOT awaited here, mirroring the @Async pipeline-runner split
 * Resume Parser Service already uses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionServiceImpl implements InterviewSessionService {

    /**
     * Fixed 8-slot category sequence covering intro + resume + 4
     * technical areas + behavioral -- HR and SCENARIO_BASED stay
     * available in the enum for a future higher question count, but
     * aren't part of the default sequence.
     */
    private static final List<QuestionCategory> CATEGORY_SEQUENCE = List.of(
            QuestionCategory.INTRODUCTION, QuestionCategory.RESUME, QuestionCategory.JAVA,
            QuestionCategory.SPRING_BOOT, QuestionCategory.MICROSERVICES, QuestionCategory.SYSTEM_DESIGN,
            QuestionCategory.SQL, QuestionCategory.BEHAVIORAL);

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewRecommendationRepository interviewRecommendationRepository;
    private final InterviewLogRepository interviewLogRepository;
    private final CandidateShortlistRepository candidateShortlistRepository;

    private final JobServiceClient jobServiceClient;
    private final ResumeParserServiceClient resumeParserServiceClient;
    private final AIInterviewProviderFactory providerFactory;
    private final AiInterviewEventProducer eventProducer;
    private final InterviewEvaluationService interviewEvaluationService;

    @Value("${ai-interview.session.default-question-count:8}")
    private int defaultQuestionCount;

    @Value("${ai-interview.session.ttl-hours:72}")
    private int ttlHours;

    @Value("${cadence.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void inviteCandidate(UUID applicationId) {
        CandidateShortlist shortlist = candidateShortlistRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHORTLIST_NOT_FOUND, "No shortlist record found for application " + applicationId));
        InterviewSession session = interviewSessionRepository.findByApplicationId(applicationId)
                .orElseGet(() -> InterviewSession.builder()
                        .applicationId(applicationId).jobId(shortlist.getJobId()).candidateId(shortlist.getCandidateId())
                        .totalQuestions(defaultQuestionCount)
                        .status(InterviewSessionStatus.NOT_STARTED)
                        .build());
        session.setInvitedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        if (session.getStatus() == InterviewSessionStatus.EXPIRED) {
            session.setStatus(InterviewSessionStatus.NOT_STARTED);
        }
        session = interviewSessionRepository.save(session);
        writeLog(session, LogLevel.INFO, "Interview invitation sent to candidate");
        publishInvited(session);
    }

    @Override
    @Transactional
    public void sendReminder(UUID applicationId) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        if (session.getStatus() != InterviewSessionStatus.NOT_STARTED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_NOT_IN_PROGRESS, "Only a not-started interview can be reminded");
        }
        // Notification Service does not exist yet in this platform -- this
        // is a validated no-op today, logged for audit/traceability.
        writeLog(session, LogLevel.INFO, "Reminder requested (Notification Service not yet available)");
    }

    @Override
    @Transactional
    public void resendInvite(UUID applicationId) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        if (session.getStatus() != InterviewSessionStatus.EXPIRED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_NOT_IN_PROGRESS, "Only an expired interview can be resent");
        }
        session.setStatus(InterviewSessionStatus.NOT_STARTED);
        session.setInvitedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        session = interviewSessionRepository.save(session);
        writeLog(session, LogLevel.INFO, "Interview invitation resent");
        publishInvited(session);
    }

    @Override
    @Transactional
    public InterviewQuestionResponse startInterview(UUID applicationId, UUID candidateId, InterviewMode mode) {
        InterviewSession session = findByApplicationIdForCandidateOrThrow(applicationId, candidateId);
        expireIfPastDeadline(session);
        if (session.getStatus() != InterviewSessionStatus.NOT_STARTED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_ALREADY_STARTED, "This interview has already been started or completed");
        }

        session.setMode(mode);
        session.setStatus(InterviewSessionStatus.IN_PROGRESS);
        session.setStartedAt(LocalDateTime.now());
        session.setCurrentQuestionIndex(0);
        interviewSessionRepository.save(session);
        writeLog(session, LogLevel.INFO, "Interview started in " + mode + " mode");

        eventProducer.publishInterviewStarted(InterviewStartedEvent.builder()
                .applicationId(applicationId).jobId(session.getJobId()).candidateId(session.getCandidateId())
                .sessionId(session.getId()).mode(mode).occurredAt(LocalDateTime.now()).build());

        return generateAndPersistQuestion(session, List.of());
    }

    @Override
    @Transactional
    public InterviewQuestionResponse submitAnswer(UUID applicationId, UUID candidateId, AnswerRequest request) {
        InterviewSession session = findByApplicationIdForCandidateOrThrow(applicationId, candidateId);
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_NOT_IN_PROGRESS, "This interview is not currently in progress");
        }
        InterviewQuestion question = interviewQuestionRepository.findById(request.getQuestionId())
                .filter(q -> q.getSessionId().equals(session.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.QUESTION_NOT_FOUND, "Question not found for this interview"));
        if (interviewAnswerRepository.findByQuestionId(question.getId()).isPresent()) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_ALREADY_COMPLETED, "This question has already been answered");
        }

        interviewAnswerRepository.save(InterviewAnswer.builder()
                .questionId(question.getId()).sessionId(session.getId())
                .answerText(request.getAnswerText()).responseTimeSeconds(request.getResponseTimeSeconds())
                .answeredAt(LocalDateTime.now()).build());

        int nextIndex = session.getCurrentQuestionIndex() + 1;
        session.setCurrentQuestionIndex(nextIndex);

        if (nextIndex >= session.getTotalQuestions()) {
            completeSession(session);
            return InterviewQuestionResponse.builder().interviewCompleted(true).totalQuestions(session.getTotalQuestions()).build();
        }

        interviewSessionRepository.save(session);
        List<QaPair> priorQa = buildPriorQaPairs(session.getId());
        return generateAndPersistQuestion(session, priorQa);
    }

    @Override
    @Transactional
    public void finishInterview(UUID applicationId, UUID candidateId) {
        InterviewSession session = findByApplicationIdForCandidateOrThrow(applicationId, candidateId);
        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_NOT_IN_PROGRESS, "This interview is not currently in progress");
        }
        completeSession(session);
    }

    @Override
    @Transactional
    public void recordRecruiterDecision(UUID applicationId, HiringRecommendation override) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        InterviewRecommendation recommendation = interviewRecommendationRepository.findBySessionId(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "No evaluation found yet for this interview"));
        recommendation.setHiringRecommendation(override);
        interviewRecommendationRepository.save(recommendation);

        eventProducer.publishCandidateRecommended(CandidateRecommendedEvent.builder()
                .applicationId(applicationId).jobId(session.getJobId()).candidateId(session.getCandidateId())
                .hiringRecommendation(override).occurredAt(LocalDateTime.now()).build());
    }

    private void completeSession(InterviewSession session) {
        session.setStatus(InterviewSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        interviewSessionRepository.save(session);
        writeLog(session, LogLevel.INFO, "Interview completed");

        eventProducer.publishInterviewCompleted(InterviewCompletedEvent.builder()
                .applicationId(session.getApplicationId()).jobId(session.getJobId()).candidateId(session.getCandidateId())
                .sessionId(session.getId()).occurredAt(LocalDateTime.now()).build());

        interviewEvaluationService.evaluate(session.getId());
    }

    private InterviewQuestionResponse generateAndPersistQuestion(InterviewSession session, List<QaPair> priorQa) {
        CandidateResumeSnapshot resumeSnapshot = buildResumeSnapshot(session.getApplicationId());
        JobContextSnapshot jobSnapshot = buildJobSnapshot(session.getJobId());

        int questionNumber = session.getCurrentQuestionIndex() + 1;
        QuestionCategory category = CATEGORY_SEQUENCE.get(Math.min(session.getCurrentQuestionIndex(), CATEGORY_SEQUENCE.size() - 1));

        InterviewQuestionContext context = new InterviewQuestionContext(
                resumeSnapshot, jobSnapshot, category, questionNumber, session.getTotalQuestions(), priorQa);

        GeneratedQuestion generated = providerFactory.getActiveProvider().generateNextQuestion(context);

        InterviewQuestion question = InterviewQuestion.builder()
                .sessionId(session.getId()).questionOrder(questionNumber).category(category)
                .questionText(generated.questionText()).askedAt(LocalDateTime.now()).build();
        interviewQuestionRepository.save(question);

        return InterviewQuestionResponse.builder()
                .questionId(question.getId()).questionOrder(questionNumber).totalQuestions(session.getTotalQuestions())
                .category(category).questionText(generated.questionText()).interviewCompleted(false).build();
    }

    private List<QaPair> buildPriorQaPairs(UUID sessionId) {
        List<InterviewQuestion> questions = interviewQuestionRepository.findAllBySessionIdOrderByQuestionOrderAsc(sessionId);
        List<QaPair> pairs = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            interviewAnswerRepository.findByQuestionId(q.getId())
                    .ifPresent(a -> pairs.add(new QaPair(q.getQuestionText(), a.getAnswerText())));
        }
        return pairs;
    }

    private CandidateResumeSnapshot buildResumeSnapshot(UUID applicationId) {
        ResumeMatchDto match = resumeParserServiceClient.getResumeMatch(applicationId).getData();
        if (match == null) {
            return new CandidateResumeSnapshot(null, null, List.of(), List.of());
        }
        List<String> skills = match.getMatchedSkills() == null ? List.of()
                : match.getMatchedSkills().stream().map(MatchedSkillDto::getSkillName).toList();
        return new CandidateResumeSnapshot(match.getFullName(), match.getProfessionalSummary(), skills, List.of());
    }

    private JobContextSnapshot buildJobSnapshot(UUID jobId) {
        JobDetailDto job = jobServiceClient.getJobDetail(jobId).getData();
        if (job == null || job.getRequirements() == null || job.getRequirements().getSkills() == null) {
            return new JobContextSnapshot(job != null ? job.getTitle() : null, List.of());
        }
        List<String> required = job.getRequirements().getSkills().stream()
                .filter(s -> "REQUIRED".equalsIgnoreCase(s.getSkillType()))
                .map(JobSkillDto::getSkillName).toList();
        return new JobContextSnapshot(job.getTitle(), required);
    }

    private void expireIfPastDeadline(InterviewSession session) {
        if (session.getStatus() == InterviewSessionStatus.NOT_STARTED
                && session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(InterviewSessionStatus.EXPIRED);
            interviewSessionRepository.save(session);
            throw new InterviewConflictException(ErrorCode.INTERVIEW_EXPIRED, "This interview invitation has expired");
        }
    }

    private void publishInvited(InterviewSession session) {
        eventProducer.publishAiInterviewInvited(AiInterviewInvitedEvent.builder()
                .applicationId(session.getApplicationId()).jobId(session.getJobId()).candidateId(session.getCandidateId())
                .interviewLink(frontendBaseUrl + "/candidate/ai-interview/" + session.getApplicationId())
                .validFrom(session.getInvitedAt()).validUntil(session.getExpiresAt())
                .occurredAt(LocalDateTime.now()).build());
    }

    private InterviewSession findByApplicationIdOrThrow(UUID applicationId) {
        return interviewSessionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "No interview found for application " + applicationId));
    }

    /**
     * Candidate.id IS the auth User's own userId (1:1, see candidate-service's
     * Candidate entity), so comparing the JWT's userId against
     * InterviewSession.candidateId is a direct, correct ownership check --
     * not a different ID space. Returns the same "not found" the caller would
     * get for a nonexistent applicationId rather than a distinct forbidden
     * error, so a candidate probing someone else's applicationId can't even
     * confirm the interview exists.
     */
    private InterviewSession findByApplicationIdForCandidateOrThrow(UUID applicationId, UUID candidateId) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        if (!session.getCandidateId().equals(candidateId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "No interview found for application " + applicationId);
        }
        return session;
    }

    private void writeLog(InterviewSession session, LogLevel level, String message) {
        interviewLogRepository.save(InterviewLog.builder()
                .sessionId(session.getId()).logLevel(level).message(message).build());
    }
}
