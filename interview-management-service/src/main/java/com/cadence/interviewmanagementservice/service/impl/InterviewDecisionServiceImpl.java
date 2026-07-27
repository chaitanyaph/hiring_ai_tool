package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.ActivityEventType;
import com.cadence.interviewmanagementservice.dto.request.RecruiterDecisionRequest;
import com.cadence.interviewmanagementservice.entity.Interview;
import com.cadence.interviewmanagementservice.entity.InterviewActivityLog;
import com.cadence.interviewmanagementservice.exception.ErrorCode;
import com.cadence.interviewmanagementservice.exception.ResourceNotFoundException;
import com.cadence.interviewmanagementservice.kafka.event.CandidateMovedToHrEvent;
import com.cadence.interviewmanagementservice.kafka.event.CandidateRejectedEvent;
import com.cadence.interviewmanagementservice.kafka.event.CandidateSelectedEvent;
import com.cadence.interviewmanagementservice.kafka.producer.InterviewManagementEventProducer;
import com.cadence.interviewmanagementservice.repository.InterviewActivityLogRepository;
import com.cadence.interviewmanagementservice.repository.InterviewRepository;
import com.cadence.interviewmanagementservice.service.InterviewDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Module 6. No internal endpoint exists on application-service to
 * advance ApplicationStatus to REJECTED/HIRED/etc. from this service
 * (confirmed by research -- only 4 score fields exist, no status-
 * transition endpoint) -- decisions are recorded locally and published
 * as Kafka events for a future application-service consumer, same
 * "no consumer yet" posture already flagged for CandidateRecommended
 * in ai-interview-service.
 */
@Service
@RequiredArgsConstructor
public class InterviewDecisionServiceImpl implements InterviewDecisionService {

    private final InterviewRepository interviewRepository;
    private final InterviewActivityLogRepository interviewActivityLogRepository;
    private final InterviewManagementEventProducer eventProducer;

    @Override
    @Transactional
    public void recordDecision(UUID companyId, UUID interviewId, UUID actorId, RecruiterDecisionRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId));
        if (!interview.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.INTERVIEW_NOT_FOUND, "Interview not found: " + interviewId);
        }

        ActivityEventType activityType = switch (request.getDecisionType()) {
            case MOVE_TO_HR -> ActivityEventType.MOVED_TO_HR;
            case NEXT_ROUND -> ActivityEventType.NEXT_ROUND_SCHEDULED;
            case SELECT -> ActivityEventType.SELECTED;
            case REJECT -> ActivityEventType.REJECTED;
            case HOLD -> ActivityEventType.HOLD;
            case REQUEST_ANOTHER_INTERVIEW -> ActivityEventType.ANOTHER_INTERVIEW_REQUESTED;
        };

        interviewActivityLogRepository.save(InterviewActivityLog.builder()
                .interviewId(interviewId)
                .eventType(activityType)
                .actorId(actorId)
                .details(request.getNotes())
                .build());

        switch (request.getDecisionType()) {
            case MOVE_TO_HR -> eventProducer.publishCandidateMovedToHr(CandidateMovedToHrEvent.builder()
                    .applicationId(interview.getApplicationId())
                    .candidateId(interview.getCandidateId())
                    .build());
            case SELECT -> eventProducer.publishCandidateSelected(CandidateSelectedEvent.builder()
                    .applicationId(interview.getApplicationId())
                    .candidateId(interview.getCandidateId())
                    .build());
            case REJECT -> eventProducer.publishCandidateRejected(CandidateRejectedEvent.builder()
                    .applicationId(interview.getApplicationId())
                    .candidateId(interview.getCandidateId())
                    .reason(request.getNotes())
                    .build());
            case NEXT_ROUND, HOLD, REQUEST_ANOTHER_INTERVIEW -> { /* logged only -- no dedicated event topic requested for these */ }
        }
    }
}
