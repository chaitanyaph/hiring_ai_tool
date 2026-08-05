package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import com.cadence.aiinterviewservice.constants.NoteType;
import com.cadence.aiinterviewservice.dto.response.*;
import com.cadence.aiinterviewservice.entity.InterviewFeedbackNote;
import com.cadence.aiinterviewservice.entity.InterviewQuestion;
import com.cadence.aiinterviewservice.entity.InterviewRecommendation;
import com.cadence.aiinterviewservice.entity.InterviewScore;
import com.cadence.aiinterviewservice.entity.InterviewSession;
import com.cadence.aiinterviewservice.exception.ErrorCode;
import com.cadence.aiinterviewservice.exception.ResourceNotFoundException;
import com.cadence.aiinterviewservice.feign.ApplicationServiceClient;
import com.cadence.aiinterviewservice.feign.dto.ApplicationSummaryDto;
import com.cadence.aiinterviewservice.mapper.InterviewMapper;
import com.cadence.aiinterviewservice.repository.*;
import com.cadence.aiinterviewservice.service.InterviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQueryServiceImpl implements InterviewQueryService {

    private static final int FLAGGED_CONFIDENCE_THRESHOLD = 60;

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewScoreRepository interviewScoreRepository;
    private final InterviewRecommendationRepository interviewRecommendationRepository;
    private final InterviewFeedbackNoteRepository interviewFeedbackNoteRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final InterviewMapper interviewMapper;

    @Value("${ai-interview.session.default-question-count:8}")
    private int defaultQuestionCount;

    @Value("${ai-interview.session.default-duration-minutes:20}")
    private int defaultDurationMinutes;

    @Override
    public PagedResponse<InterviewQueueItemResponse> getQueue(UUID jobId, InterviewSessionStatus status, Pageable pageable) {
        Page<InterviewSession> page = interviewSessionRepository.search(jobId, status, pageable);
        var applications = fetchApplicationsByJob(jobId);
        return PagedResponse.from(page.map(s -> enrichQueueItem(s, applications)));
    }

    @Override
    public InterviewAnalysisSummaryResponse getAnalysisSummary(UUID jobId) {
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        long completed = interviewSessionRepository.countByJobIdAndStatus(jobId, InterviewSessionStatus.COMPLETED);
        long completedThisWeek = interviewSessionRepository.countByJobIdAndStatusAndCompletedAtAfter(jobId, InterviewSessionStatus.COMPLETED, startOfWeek);

        return InterviewAnalysisSummaryResponse.builder()
                .completedCount(completed)
                .completedThisWeekCount(completedThisWeek)
                .avgOverallScore(interviewScoreRepository.findAvgOverallScoreByJob(jobId))
                .avgCommunicationScore(interviewScoreRepository.findAvgCommunicationScoreByJob(jobId))
                .flaggedForReviewCount(interviewScoreRepository.countFlaggedForReviewByJob(jobId, FLAGGED_CONFIDENCE_THRESHOLD))
                .build();
    }

    @Override
    public PagedResponse<InterviewCompletedItemResponse> getCompletedList(UUID jobId, Pageable pageable) {
        Page<InterviewSession> page = interviewSessionRepository.findAllByJobIdAndStatusOrderByCompletedAtDesc(jobId, InterviewSessionStatus.COMPLETED, pageable);
        var applications = fetchApplicationsByJob(jobId);
        return PagedResponse.from(page.map(s -> enrichCompletedItem(s, applications)));
    }

    @Override
    public InterviewEvaluationReportResponse getReport(UUID applicationId) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        UUID sessionId = session.getId();

        InterviewEvaluationReportResponse.InterviewEvaluationReportResponseBuilder builder = InterviewEvaluationReportResponse.builder()
                .applicationId(applicationId).candidateId(session.getCandidateId()).jobId(session.getJobId())
                .completedAt(session.getCompletedAt());

        ApplicationSummaryDto application = fetchApplicationsByJob(session.getJobId()).get(applicationId);
        if (application != null) {
            builder.fullName(application.getCandidateNameSnapshot()).jobTitle(application.getJobTitleSnapshot());
        }

        interviewScoreRepository.findBySessionId(sessionId).ifPresent(score -> applyScore(builder, score));
        interviewRecommendationRepository.findBySessionId(sessionId).ifPresent(rec -> applyRecommendation(builder, rec));
        builder.strengths(noteDescriptions(sessionId, NoteType.STRENGTH));
        builder.weaknesses(noteDescriptions(sessionId, NoteType.WEAKNESS));
        builder.improvementAreas(noteDescriptions(sessionId, NoteType.IMPROVEMENT));
        builder.transcript(buildTranscript(sessionId));

        return builder.build();
    }

    @Override
    public InterviewDetailsResponse getCandidateDetails(UUID applicationId, UUID candidateId) {
        InterviewSession session = findByApplicationIdForCandidateOrThrow(applicationId, candidateId);
        ApplicationSummaryDto application = fetchApplicationsByJob(session.getJobId()).get(applicationId);

        return InterviewDetailsResponse.builder()
                .applicationId(applicationId)
                .jobTitle(application != null ? application.getJobTitleSnapshot() : null)
                .status(session.getStatus())
                .totalQuestions(session.getTotalQuestions())
                .estimatedDurationMinutes(defaultDurationMinutes)
                .modeOptions(List.of("CHAT", "VOICE", "VIDEO"))
                .expiresAt(session.getExpiresAt())
                .build();
    }

    @Override
    public InterviewResultResponse getCandidateResult(UUID applicationId, UUID candidateId) {
        InterviewSession session = findByApplicationIdForCandidateOrThrow(applicationId, candidateId);

        if (session.getStatus() != InterviewSessionStatus.COMPLETED) {
            return InterviewResultResponse.builder()
                    .status(session.getStatus())
                    .message(messageFor(session.getStatus()))
                    .build();
        }

        boolean evaluated = interviewRecommendationRepository.findBySessionId(session.getId()).isPresent();
        return InterviewResultResponse.builder()
                .status(session.getStatus())
                .message(evaluated ? "Your interview has been evaluated." : "Our AI is analyzing your responses -- you'll see your status update within a few hours.")
                .transcript(buildTranscript(session.getId()))
                .build();
    }

    private String messageFor(InterviewSessionStatus status) {
        return switch (status) {
            case NOT_STARTED -> "This interview has not started yet.";
            case IN_PROGRESS -> "This interview is still in progress.";
            case EXPIRED -> "This interview invitation has expired.";
            case COMPLETED -> "Your interview has been evaluated.";
        };
    }

    private void applyScore(InterviewEvaluationReportResponse.InterviewEvaluationReportResponseBuilder builder, InterviewScore score) {
        builder.overallScore(score.getOverallScore())
                .communicationScore(score.getCommunicationScore())
                .confidenceScore(score.getConfidenceScore())
                .technicalAccuracyScore(score.getTechnicalAccuracyScore())
                .problemSolvingScore(score.getProblemSolvingScore())
                .grammarScore(score.getGrammarScore())
                .behaviorScore(score.getBehaviorScore())
                .leadershipScore(score.getLeadershipScore())
                .domainKnowledgeScore(score.getDomainKnowledgeScore())
                .eyeContactScore(score.getEyeContactScore())
                .speakingPaceScore(score.getSpeakingPaceScore())
                .fillerWordCount(score.getFillerWordCount())
                .avgResponseLatencySeconds(score.getAvgResponseLatencySeconds());
    }

    private void applyRecommendation(InterviewEvaluationReportResponse.InterviewEvaluationReportResponseBuilder builder, InterviewRecommendation rec) {
        builder.hiringRecommendation(rec.getHiringRecommendation())
                .interviewSummary(rec.getInterviewSummary())
                .recruiterSummary(rec.getRecruiterSummary());
    }

    private List<String> noteDescriptions(UUID sessionId, NoteType noteType) {
        return interviewFeedbackNoteRepository.findAllBySessionIdAndNoteTypeOrderByDisplayOrderAsc(sessionId, noteType)
                .stream().map(InterviewFeedbackNote::getDescription).toList();
    }

    private List<TranscriptTurnResponse> buildTranscript(UUID sessionId) {
        List<InterviewQuestion> questions = interviewQuestionRepository.findAllBySessionIdOrderByQuestionOrderAsc(sessionId);
        List<TranscriptTurnResponse> transcript = new java.util.ArrayList<>();
        for (InterviewQuestion q : questions) {
            transcript.add(TranscriptTurnResponse.builder().speaker("AI").text(q.getQuestionText()).build());
            interviewAnswerRepository.findByQuestionId(q.getId())
                    .ifPresent(a -> transcript.add(TranscriptTurnResponse.builder().speaker("CANDIDATE").text(a.getAnswerText()).build()));
        }
        return transcript;
    }

    private InterviewQueueItemResponse enrichQueueItem(InterviewSession session, java.util.Map<UUID, ApplicationSummaryDto> applications) {
        InterviewQueueItemResponse item = interviewMapper.toQueueItemResponse(session);
        ApplicationSummaryDto application = applications.get(session.getApplicationId());
        if (application != null) {
            item.setFullName(application.getCandidateNameSnapshot());
            item.setEmail(application.getCandidateEmailSnapshot());
            item.setJobTitle(application.getJobTitleSnapshot());
        }
        return item;
    }

    private InterviewCompletedItemResponse enrichCompletedItem(InterviewSession session, java.util.Map<UUID, ApplicationSummaryDto> applications) {
        InterviewCompletedItemResponse.InterviewCompletedItemResponseBuilder builder = InterviewCompletedItemResponse.builder()
                .applicationId(session.getApplicationId()).candidateId(session.getCandidateId())
                .jobId(session.getJobId()).completedAt(session.getCompletedAt());

        ApplicationSummaryDto application = applications.get(session.getApplicationId());
        if (application != null) {
            builder.fullName(application.getCandidateNameSnapshot()).jobTitle(application.getJobTitleSnapshot());
        }
        interviewScoreRepository.findBySessionId(session.getId()).ifPresent(score -> builder.overallScore(score.getOverallScore()));
        interviewRecommendationRepository.findBySessionId(session.getId()).ifPresent(rec -> builder.hiringRecommendation(rec.getHiringRecommendation()));
        return builder.build();
    }

    private java.util.Map<UUID, ApplicationSummaryDto> fetchApplicationsByJob(UUID jobId) {
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return java.util.Map.of();
        }
        return applications.stream().collect(java.util.stream.Collectors.toMap(ApplicationSummaryDto::getId, a -> a, (a, b) -> a));
    }

    private InterviewSession findByApplicationIdOrThrow(UUID applicationId) {
        return interviewSessionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "No interview found for application " + applicationId));
    }

    /** See InterviewSessionServiceImpl's identical helper for why comparing candidateId to the JWT userId is a correct, direct ownership check. */
    private InterviewSession findByApplicationIdForCandidateOrThrow(UUID applicationId, UUID candidateId) {
        InterviewSession session = findByApplicationIdOrThrow(applicationId);
        if (!session.getCandidateId().equals(candidateId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "No interview found for application " + applicationId);
        }
        return session;
    }
}
