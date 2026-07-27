package com.cadence.applicationservice.service;

import com.cadence.applicationservice.constant.InterviewType;

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

    void handleCandidateShortlisted(UUID applicationId);

    void handleInterviewCompleted(UUID applicationId, InterviewType interviewType, Integer score, String feedback);

    void handleCodingAssessmentCompleted(UUID applicationId, Integer score);

    void handleBackgroundVerificationCompleted(UUID applicationId, boolean passed, String remarks);

    void handleOfferReleased(UUID applicationId, UUID offerId);
}
