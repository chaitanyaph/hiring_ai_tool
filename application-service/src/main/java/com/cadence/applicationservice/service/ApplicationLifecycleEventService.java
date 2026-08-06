package com.cadence.applicationservice.service;

import com.cadence.applicationservice.constant.HiringRecommendation;
import com.cadence.applicationservice.constant.InterviewType;
import com.cadence.applicationservice.constant.ShortlistDecision;

import java.util.UUID;

/**
 * The system-driven half of the lifecycle -- called only by
 * ApplicationEventConsumer in reaction to Kafka events from other
 * services, never directly by a controller. Kept separate from
 * ApplicationService (the user/recruiter-driven half) so the two
 * trigger sources -- a human action vs. an upstream service's event --
 * are never confused with each other, even though both ultimately
 * mutate the same Application aggregate.
 */
public interface ApplicationLifecycleEventService {

    void handleResumeParsed(UUID applicationId, UUID resumeId);

    void handleResumeMatched(UUID applicationId, Integer matchScore);

    void handleCandidateShortlisted(UUID applicationId, ShortlistDecision decision, Integer overallMatchScore);

    /**
     * recommendation is only meaningful for interviewType == AI (ai-interview-service's
     * InterviewEvaluatedEvent is the only source of a PROCEED/HOLD/REJECT signal) --
     * pass null for TECHNICAL/MANAGER/HR, whose completion event carries no such field.
     */
    void handleInterviewCompleted(UUID applicationId, InterviewType interviewType, Integer score, String feedback, HiringRecommendation recommendation);

    void handleCodingAssessmentCompleted(UUID applicationId, Integer score, Boolean passed);

    void handleBackgroundVerificationCompleted(UUID applicationId, boolean passed, String remarks);

    void handleOfferReleased(UUID applicationId, UUID offerId);
}
