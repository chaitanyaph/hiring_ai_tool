package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.ActivityEventType;
import com.cadence.interviewmanagementservice.constants.ApplicationInterviewType;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import com.cadence.interviewmanagementservice.dto.request.SubmitFeedbackRequest;
import com.cadence.interviewmanagementservice.dto.response.InterviewFeedbackResponse;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.entity.InterviewActivityLog;
import com.cadence.interviewmanagementservice.entity.InterviewFeedback;
import com.cadence.interviewmanagementservice.entity.InterviewPanelist;
import com.cadence.interviewmanagementservice.exception.AccessDeniedApiException;
import com.cadence.interviewmanagementservice.exception.ErrorCode;
import com.cadence.interviewmanagementservice.exception.InterviewConflictException;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.kafka.event.ApplicationInterviewCompletedEvent;
import com.cadence.interviewmanagementservice.kafka.event.InterviewCompletedEvent;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.mapper.InterviewFeedbackMapper;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewFeedbackRepository;
import com.cadence.interviewmanagementservice.repository.InterviewPanelistRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import com.cadence.interviewmanagementservice.service.InterviewFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists the full 7-dimension feedback model, but also aggregates
 * the 3 dimensions the Figma form actually collects (communication/
 * technical/culture fit) into a single score + concatenated text for
 * the bridge event onto application-service's existing
 * interview.interview.completed topic -- that consumer only accepts
 * one aggregate score/feedback pair per event (see §B3 research),
 * not this service's richer breakdown.
 */
@Service
@RequiredArgsConstructor
public class InterviewFeedbackServiceImpl implements InterviewFeedbackService {

    private static final Map<RoundType, ApplicationInterviewType> ROUND_TYPE_BRIDGE = Map.of(
            RoundType.TECHNICAL, ApplicationInterviewType.TECHNICAL,
            RoundType.MANAGER, ApplicationInterviewType.MANAGER,
            RoundType.HR, ApplicationInterviewType.HR,
            // ARCHITECT/CUSTOM don't exist on application-service's InterviewType --
            // mapped down to TECHNICAL, flagged in the README, not silently assumed correct.
            RoundType.ARCHITECT, ApplicationInterviewType.TECHNICAL,
            RoundType.CUSTOM, ApplicationInterviewType.TECHNICAL
    );

    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;
    private final InterviewPanelistRepository interviewPanelistRepository;
    private final InterviewActivityLogRepository interviewActivityLogRepository;
    private final InterviewFeedbackMapper interviewFeedbackMapper;
    private final InterviewManagementEventProducer eventProducer;
    private final CandidateTimelineService candidateTimelineService;

    @Override
    @Transactional
    public InterviewFeedbackResponse submitFeedback(UUID companyId, UUID interviewId, UUID interviewerId,
                                                      boolean callerIsRecruitingRole, SubmitFeedbackRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId);
        }
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new InterviewConflictException(ErrorCode.INTERVIEW_NOT_COMPLETABLE, "Cannot submit feedback for a cancelled interview");
        }
        if (interviewFeedbackRepository.findByInterviewIdAndInterviewerId(interviewId, interviewerId).isPresent()) {
            throw new InterviewConflictException(ErrorCode.FEEDBACK_ALREADY_SUBMITTED, "Feedback already submitted for this interview by this interviewer");
        }

        boolean isPanelist = interviewPanelistRepository.existsByInterviewIdAndInterviewerId(interviewId, interviewerId);
        if (!isPanelist && !callerIsRecruitingRole) {
            throw new AccessDeniedApiException("Only an assigned panelist or a recruiter may submit feedback for this interview");
        }

        InterviewFeedback feedback = InterviewFeedback.builder()
                .interviewId(interviewId)
                .interviewerId(interviewerId)
                .communicationScore(request.getCommunicationScore())
                .technicalScore(request.getTechnicalScore())
                .cultureFitScore(request.getCultureFitScore())
                .codingSkillsScore(request.getCodingSkillsScore())
                .problemSolvingScore(request.getProblemSolvingScore())
                .systemDesignScore(request.getSystemDesignScore())
                .leadershipScore(request.getLeadershipScore())
                .overallRating(request.getOverallRating())
                .strengths(request.getStrengths())
                .weaknesses(request.getWeaknesses())
                .comments(request.getComments())
                .recommendation(request.getRecommendation())
                .submittedAt(LocalDateTime.now())
                .build();
        feedback = interviewFeedbackRepository.save(feedback);

        if (isPanelist) {
            InterviewPanelist panelist = interviewPanelistRepository.findAllByInterviewId(interviewId).stream()
                    .filter(p -> p.getInterviewerId().equals(interviewerId))
                    .findFirst().orElseThrow();
            panelist.setFeedbackSubmitted(true);
            interviewPanelistRepository.save(panelist);
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interviewRepository.save(interview);

        interviewActivityLogRepository.save(InterviewActivityLog.builder()
                .interviewId(interviewId)
                .eventType(ActivityEventType.FEEDBACK_SUBMITTED)
                .actorId(interviewerId)
                .details("Recommendation: " + request.getRecommendation())
                .build());

        candidateTimelineService.markStageDone(interview.getApplicationId(), interview.getCandidateId(), toTimelineStage(interview.getRoundType()));

        eventProducer.publishInterviewCompleted(InterviewCompletedEvent.builder()
                .interviewId(interview.getId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .roundType(interview.getRoundType())
                .overallRating(request.getOverallRating())
                .recommendation(request.getRecommendation())
                .build());

        eventProducer.publishApplicationInterviewCompleted(ApplicationInterviewCompletedEvent.builder()
                .applicationId(interview.getApplicationId())
                .interviewType(ROUND_TYPE_BRIDGE.getOrDefault(interview.getRoundType(), ApplicationInterviewType.TECHNICAL))
                .score(aggregateScore(request))
                .feedback(aggregateFeedbackText(request))
                .build());

        InterviewFeedbackResponse response = interviewFeedbackMapper.toResponse(feedback);
        return response;
    }

    @Override
    public List<InterviewFeedbackResponse> getFeedback(UUID companyId, UUID interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId);
        }
        return interviewFeedbackRepository.findAllByInterviewId(interviewId).stream()
                .map(interviewFeedbackMapper::toResponse)
                .toList();
    }

    private Integer aggregateScore(SubmitFeedbackRequest request) {
        // Scaled from the Figma's 1-10 fields to a 0-100 score, matching
        // the 0-100 scale ScoreUpdateRequest uses everywhere else on
        // application-service's side.
        List<Integer> scores = List.of(request.getCommunicationScore(), request.getTechnicalScore(), request.getCultureFitScore())
                .stream().filter(java.util.Objects::nonNull).toList();
        if (scores.isEmpty()) {
            return null;
        }
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return (int) Math.round(avg * 10);
    }

    private String aggregateFeedbackText(SubmitFeedbackRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getStrengths() != null && !request.getStrengths().isBlank()) {
            sb.append("Strengths: ").append(request.getStrengths());
        }
        if (request.getWeaknesses() != null && !request.getWeaknesses().isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Concerns: ").append(request.getWeaknesses());
        }
        return sb.length() > 0 ? sb.toString() : ("Recommendation: " + request.getRecommendation());
    }

    private com.cadence.interviewmanagementservice.constants.TimelineStage toTimelineStage(RoundType roundType) {
        return switch (roundType) {
            case TECHNICAL, ARCHITECT, CUSTOM -> com.cadence.interviewmanagementservice.constants.TimelineStage.TECHNICAL_INTERVIEW;
            case MANAGER -> com.cadence.interviewmanagementservice.constants.TimelineStage.MANAGER_INTERVIEW;
            case HR -> com.cadence.interviewmanagementservice.constants.TimelineStage.HR_INTERVIEW;
        };
    }
}
