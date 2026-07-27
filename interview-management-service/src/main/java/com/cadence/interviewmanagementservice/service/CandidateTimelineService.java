package com.cadence.interviewmanagementservice.service;

import com.cadence.interviewmanagementservice.dto.response.CandidateTimelineResponse;
import com.cadence.interviewmanagementservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.interviewmanagementservice.kafka.event.CodingAssessmentCompletedEvent;

import java.util.List;
import java.util.UUID;

public interface CandidateTimelineService {

    void handleCandidateRecommended(CandidateRecommendedEvent event);

    void handleCodingAssessmentCompleted(CodingAssessmentCompletedEvent event);

    void markStageDone(UUID applicationId, UUID candidateId, com.cadence.interviewmanagementservice.constants.TimelineStage stage);

    List<CandidateTimelineResponse> getTimelineForApplication(UUID applicationId);

    List<CandidateTimelineResponse> getMyTimeline(UUID candidateId, UUID applicationId);
}
