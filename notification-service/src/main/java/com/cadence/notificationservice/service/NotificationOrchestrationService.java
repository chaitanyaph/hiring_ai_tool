package com.cadence.notificationservice.service;

import com.cadence.notificationservice.kafka.event.*;

/** One handler method per real consumed Kafka event (§9 of the architecture doc) -- resolves template/recipient, enriches, queues email and/or creates an in-app notification. */
public interface NotificationOrchestrationService {

    void handleCompanyCreated(CompanyCreatedEvent event);

    void handleTeamInvitationCreated(TeamInvitationCreatedEvent event);

    void handleJobPublished(JobPublishedEvent event);

    void handleJobClosed(JobClosedEvent event);

    void handleUserRegistered(UserRegisteredEvent event);

    void handlePasswordResetRequested(PasswordResetRequestedEvent event);

    void handleApplicationCreated(ApplicationCreatedEvent event);

    void handleResumeUploaded(ResumeUploadedEvent event);

    void handleResumeAnalyzed(ResumeAnalyzedEvent event);

    void handleCandidateShortlisted(CandidateShortlistedEvent event);

    void handleAiInterviewInvited(AiInterviewInvitedEvent event);

    void handleAiInterviewCompleted(AiInterviewCompletedEvent event);

    void handleCodingAssessmentInvited(CodingAssessmentInvitedEvent event);

    void handleCodingAssessmentCompleted(CodingAssessmentCompletedEvent event);

    void handleInterviewScheduled(InterviewScheduledEvent event);

    void handleInterviewRescheduled(InterviewRescheduledEvent event);

    void handleInterviewCancelled(InterviewCancelledEvent event);

    void handleCandidateMovedToHr(CandidateMovedToHrEvent event);

    void handleCandidateSelected(CandidateSelectedEvent event);

    void handleCandidateRejected(CandidateRejectedEvent event);

    void handleOfferAccepted(OfferAcceptedEvent event);

    void handleOfferRejected(OfferRejectedEvent event);
}
