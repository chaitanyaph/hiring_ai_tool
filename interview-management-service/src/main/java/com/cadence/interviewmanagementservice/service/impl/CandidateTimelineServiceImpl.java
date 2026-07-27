package com.cadence.interviewmanagementservice.service.impl;

import com.cadence.interviewmanagementservice.constants.TimelineStage;
import com.cadence.interviewmanagementservice.constants.TimelineStatus;
import com.cadence.interviewmanagementservice.dto.response.CandidateTimelineResponse;
import com.cadence.interviewmanagementservice.entity.CandidateTimeline;
import com.cadence.interviewmanagementservice.exception.AccessDeniedApiException;
import com.cadence.interviewmanagementservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.interviewmanagementservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.interviewmanagementservice.mapper.CandidateTimelineMapper;
import com.cadence.interviewmanagementservice.repository.CandidateTimelineRepository;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Populated only for the stages this service can actually observe:
 * AI_INTERVIEW/CODING_ASSESSMENT from the two consumed Kafka events,
 * TECHNICAL_INTERVIEW/MANAGER_INTERVIEW/HR_INTERVIEW from this
 * service's own scheduling actions. APPLICATION/AI_RESUME_SCREENING
 * are not populated (no Feign client or event source for them exists
 * in this service's scope) -- flagged in the README.
 */
@Service
@RequiredArgsConstructor
public class CandidateTimelineServiceImpl implements CandidateTimelineService {

    private final CandidateTimelineRepository candidateTimelineRepository;
    private final CandidateTimelineMapper candidateTimelineMapper;

    @Override
    @Transactional
    public void handleCandidateRecommended(CandidateRecommendedEvent event) {
        upsert(event.getApplicationId(), event.getCandidateId(), TimelineStage.AI_INTERVIEW,
                event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now(), null,
                "Recommendation: " + event.getHiringRecommendation());
    }

    @Override
    @Transactional
    public void handleCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        upsert(event.getApplicationId(), null, TimelineStage.CODING_ASSESSMENT,
                LocalDateTime.now(), event.getScore(), null);
    }

    @Override
    @Transactional
    public void markStageDone(UUID applicationId, UUID candidateId, TimelineStage stage) {
        upsert(applicationId, candidateId, stage, LocalDateTime.now(), null, null);
    }

    @Override
    public List<CandidateTimelineResponse> getTimelineForApplication(UUID applicationId) {
        return candidateTimelineRepository.findAllByApplicationId(applicationId).stream()
                .map(candidateTimelineMapper::toResponse)
                .toList();
    }

    @Override
    public List<CandidateTimelineResponse> getMyTimeline(UUID candidateId, UUID applicationId) {
        List<CandidateTimeline> rows = candidateTimelineRepository.findAllByApplicationId(applicationId);
        boolean owned = rows.isEmpty() || rows.stream().allMatch(r -> r.getCandidateId() == null || r.getCandidateId().equals(candidateId));
        if (!owned) {
            throw new AccessDeniedApiException("This timeline does not belong to you");
        }
        return rows.stream().map(candidateTimelineMapper::toResponse).toList();
    }

    private void upsert(UUID applicationId, UUID candidateId, TimelineStage stage, LocalDateTime occurredAt, Integer score, String note) {
        CandidateTimeline row = candidateTimelineRepository.findByApplicationIdAndStage(applicationId, stage)
                .orElseGet(() -> CandidateTimeline.builder()
                        .applicationId(applicationId)
                        .candidateId(candidateId)
                        .stage(stage)
                        .build());
        if (candidateId != null) {
            row.setCandidateId(candidateId);
        }
        row.setStatus(TimelineStatus.DONE);
        row.setOccurredAt(occurredAt);
        if (score != null) {
            row.setScore(score);
        }
        if (note != null) {
            row.setNote(note);
        }
        row.setUpdatedAt(LocalDateTime.now());
        candidateTimelineRepository.save(row);
    }
}
