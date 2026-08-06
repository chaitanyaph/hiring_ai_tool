package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import com.cadence.aiinterviewservice.constants.LogLevel;
import com.cadence.aiinterviewservice.constants.NoteType;
import com.cadence.aiinterviewservice.entity.*;
import com.cadence.aiinterviewservice.feign.ApplicationServiceClient;
import com.cadence.aiinterviewservice.feign.JobServiceClient;
import com.cadence.aiinterviewservice.feign.ResumeParserServiceClient;
import com.cadence.aiinterviewservice.feign.dto.JobDetailDto;
import com.cadence.aiinterviewservice.feign.dto.JobSkillDto;
import com.cadence.aiinterviewservice.feign.dto.MatchedSkillDto;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import com.cadence.aiinterviewservice.feign.dto.ScoreUpdateRequest;
import com.cadence.aiinterviewservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.aiinterviewservice.kafka.event.InterviewEvaluatedEvent;
import com.cadence.aiinterviewservice.kafka.producer.AiInterviewEventProducer;
import com.cadence.aiinterviewservice.provider.CandidateResumeSnapshot;
import com.cadence.aiinterviewservice.provider.InterviewEvaluationContext;
import com.cadence.aiinterviewservice.provider.InterviewEvaluationData;
import com.cadence.aiinterviewservice.provider.JobContextSnapshot;
import com.cadence.aiinterviewservice.provider.TranscriptTurn;
import com.cadence.aiinterviewservice.repository.*;
import com.cadence.aiinterviewservice.service.InterviewEvaluationService;
import com.cadence.aiinterviewservice.strategy.AIInterviewProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The async evaluation pipeline (assemble transcript -> AI evaluate ->
 * persist 3 tables -> write back to Application Service -> publish),
 * triggered by InterviewSessionServiceImpl once a session reaches
 * COMPLETED. @Async works safely here without a separate pipeline-
 * runner bean (unlike Resume Parser Service's split) because it's
 * always invoked from a *different* bean (InterviewSessionServiceImpl),
 * so Spring's AOP proxy for this bean intercepts the call correctly --
 * no self-invocation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewEvaluationServiceImpl implements InterviewEvaluationService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final InterviewRecommendationRepository interviewRecommendationRepository;
    private final InterviewFeedbackNoteRepository interviewFeedbackNoteRepository;
    private final InterviewLogRepository interviewLogRepository;

    private final JobServiceClient jobServiceClient;
    private final ResumeParserServiceClient resumeParserServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final AIInterviewProviderFactory providerFactory;
    private final AiInterviewEventProducer eventProducer;

    @Override
    @Async
    @Transactional
    public void evaluate(UUID sessionId) {
        Optional<InterviewSession> maybeSession = interviewSessionRepository.findById(sessionId);
        if (maybeSession.isEmpty()) {
            return;
        }
        InterviewSession session = maybeSession.get();
        try {
            List<TranscriptTurn> transcript = buildTranscript(sessionId);
            CandidateResumeSnapshot resumeSnapshot = buildResumeSnapshot(session.getApplicationId());
            JobContextSnapshot jobSnapshot = buildJobSnapshot(session.getJobId());

            InterviewEvaluationData data = providerFactory.getActiveProvider()
                    .evaluateInterview(new InterviewEvaluationContext(resumeSnapshot, jobSnapshot, transcript));

            int fillerWordCount = computeFillerWordCount(transcript);
            Integer avgResponseLatencySeconds = computeAvgResponseLatencySeconds(session.getId());
            HiringRecommendation recommendation = toHiringRecommendation(data.hiringRecommendation());
            persistEvaluation(session, data, recommendation, fillerWordCount, avgResponseLatencySeconds);
            writeLog(session, LogLevel.INFO, "Evaluation completed");

            applicationServiceClient.updateInterviewScore(session.getApplicationId(),
                    ScoreUpdateRequest.builder().score(data.overallScore()).source("ai-interview-service").build());

            eventProducer.publishInterviewEvaluated(InterviewEvaluatedEvent.builder()
                    .applicationId(session.getApplicationId()).jobId(session.getJobId()).candidateId(session.getCandidateId())
                    .sessionId(session.getId()).overallScore(data.overallScore())
                    .hiringRecommendation(recommendation)
                    .occurredAt(LocalDateTime.now()).build());

            eventProducer.publishCandidateRecommended(CandidateRecommendedEvent.builder()
                    .applicationId(session.getApplicationId()).jobId(session.getJobId()).candidateId(session.getCandidateId())
                    .hiringRecommendation(recommendation)
                    .occurredAt(LocalDateTime.now()).build());

        } catch (Exception e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Evaluation failed for session {}: {}", sessionId, reason, e);
            writeLog(session, LogLevel.ERROR, "Evaluation failed: " + reason);
        }
    }

    private static final java.util.regex.Pattern FILLER_WORD_PATTERN = java.util.regex.Pattern.compile(
            "\\b(um+|uh+|like|you know|sort of|kind of|actually|basically|literally|i mean)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /** Real, deterministic count from the candidate's actual transcribed text -- no LLM guessing involved. */
    private int computeFillerWordCount(List<TranscriptTurn> transcript) {
        if (transcript == null) {
            return 0;
        }
        int count = 0;
        for (TranscriptTurn turn : transcript) {
            if (!"CANDIDATE".equals(turn.speaker()) || turn.text() == null) {
                continue;
            }
            java.util.regex.Matcher matcher = FILLER_WORD_PATTERN.matcher(turn.text());
            while (matcher.find()) {
                count++;
            }
        }
        return count;
    }

    /** Real average of each answer's browser-side timer value -- null (not a guess) if none were recorded. */
    private Integer computeAvgResponseLatencySeconds(UUID sessionId) {
        List<Integer> times = interviewAnswerRepository.findAllBySessionIdOrderByAnsweredAtAsc(sessionId).stream()
                .map(InterviewAnswer::getResponseTimeSeconds)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (times.isEmpty()) {
            return null;
        }
        return (int) Math.round(times.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private List<TranscriptTurn> buildTranscript(UUID sessionId) {
        List<InterviewQuestion> questions = interviewQuestionRepository.findAllBySessionIdOrderByQuestionOrderAsc(sessionId);
        List<TranscriptTurn> transcript = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            transcript.add(new TranscriptTurn("AI", q.getQuestionText()));
            interviewAnswerRepository.findByQuestionId(q.getId())
                    .ifPresent(a -> transcript.add(new TranscriptTurn("CANDIDATE", a.getAnswerText())));
        }
        return transcript;
    }

    @Transactional
    protected void persistEvaluation(InterviewSession session, InterviewEvaluationData data, HiringRecommendation recommendation, int fillerWordCount, Integer avgResponseLatencySeconds) {
        UUID sessionId = session.getId();
        interviewScoreRepository.deleteBySessionId(sessionId);
        interviewRecommendationRepository.deleteBySessionId(sessionId);
        interviewFeedbackNoteRepository.deleteBySessionId(sessionId);

        interviewScoreRepository.save(InterviewScore.builder()
                .sessionId(sessionId)
                .communicationScore(data.communicationScore())
                .confidenceScore(data.confidenceScore())
                .technicalAccuracyScore(data.technicalAccuracyScore())
                .problemSolvingScore(data.problemSolvingScore())
                .grammarScore(data.grammarScore())
                .behaviorScore(data.behaviorScore())
                .leadershipScore(data.leadershipScore())
                .domainKnowledgeScore(data.domainKnowledgeScore())
                .overallScore(data.overallScore())
                // No real audio/video capture pipeline exists on this platform, so these
                // two are left null (honest "not available") instead of an LLM-fabricated
                // guess -- see InterviewEvaluationData's class comment.
                .eyeContactScore(null)
                .speakingPaceScore(null)
                .fillerWordCount(fillerWordCount)
                .avgResponseLatencySeconds(avgResponseLatencySeconds)
                .build());

        interviewRecommendationRepository.save(InterviewRecommendation.builder()
                .sessionId(sessionId)
                .hiringRecommendation(recommendation)
                .interviewSummary(data.interviewSummary())
                .recruiterSummary(data.recruiterSummary())
                .build());

        List<InterviewFeedbackNote> notes = new ArrayList<>();
        int order = 0;
        if (data.strengths() != null) {
            for (String s : data.strengths()) {
                notes.add(InterviewFeedbackNote.builder().sessionId(sessionId).noteType(NoteType.STRENGTH).description(s).displayOrder(order++).build());
            }
        }
        order = 0;
        if (data.weaknesses() != null) {
            for (String s : data.weaknesses()) {
                notes.add(InterviewFeedbackNote.builder().sessionId(sessionId).noteType(NoteType.WEAKNESS).description(s).displayOrder(order++).build());
            }
        }
        order = 0;
        if (data.improvementAreas() != null) {
            for (String s : data.improvementAreas()) {
                notes.add(InterviewFeedbackNote.builder().sessionId(sessionId).noteType(NoteType.IMPROVEMENT).description(s).displayOrder(order++).build());
            }
        }
        interviewFeedbackNoteRepository.saveAll(notes);
    }

    private HiringRecommendation toHiringRecommendation(String raw) {
        if (raw == null) {
            return HiringRecommendation.HOLD;
        }
        try {
            return HiringRecommendation.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HiringRecommendation.HOLD;
        }
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

    private void writeLog(InterviewSession session, LogLevel level, String message) {
        interviewLogRepository.save(InterviewLog.builder()
                .sessionId(session.getId()).logLevel(level).message(message).build());
    }
}
