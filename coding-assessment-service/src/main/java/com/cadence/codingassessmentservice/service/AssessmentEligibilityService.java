package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.kafka.event.CandidateRecommendedEvent;

import java.util.List;
import java.util.UUID;

/** Orchestration / write side of eligibility tracking: the CandidateRecommended trigger, and the query used when publishing an assessment invites eligible candidates. */
public interface AssessmentEligibilityService {

    void handleCandidateRecommended(CandidateRecommendedEvent event);

    List<EligibleCandidate> findUninvitedEligibleCandidates(UUID jobId, UUID assessmentId);
}
