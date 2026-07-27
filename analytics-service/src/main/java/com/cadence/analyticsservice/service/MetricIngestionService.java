package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.kafka.event.*;

/** One handler per real consumed event -- the core aggregation engine, see architecture doc §10 "KPI Calculation Strategy". */
public interface MetricIngestionService {

    void onCompanyCreated(CompanyCreatedEvent event);

    void onJobPublished(JobPublishedEvent event);

    void onJobClosed(JobClosedEvent event);

    void onUserRegistered(UserRegisteredEvent event);

    void onApplicationCreated(ApplicationCreatedEvent event);

    void onApplicationStatusChanged(ApplicationStatusChangedEvent event);

    void onRecruiterAssigned(RecruiterAssignedEvent event);

    void onResumeParsed(ResumeParsedEvent event);

    void onResumeAnalyzed(ResumeAnalyzedEvent event);

    void onCandidateShortlisted(CandidateShortlistedEvent event);

    void onInterviewEvaluated(InterviewEvaluatedEvent event);

    void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event);

    void onInterviewCompleted(InterviewManagementInterviewCompletedEvent event);

    void onInterviewCancelled(InterviewManagementInterviewCancelledEvent event);

    void onOfferGenerated(OfferGeneratedEvent event);

    void onOfferSent(OfferSentEvent event);

    void onOfferAccepted(OfferAcceptedEvent event);

    void onOfferRejected(OfferRejectedEvent event);

    void onOfferNegotiationRequested(OfferNegotiationRequestedEvent event);
}
