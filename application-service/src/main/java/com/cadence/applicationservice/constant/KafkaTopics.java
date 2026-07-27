package com.cadence.applicationservice.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Published by this service ----
    public static final String APPLICATION_CREATED = "application.application.created";
    public static final String APPLICATION_WITHDRAWN = "application.application.withdrawn";
    public static final String APPLICATION_STATUS_CHANGED = "application.status.changed";
    public static final String RECRUITER_ASSIGNED = "application.recruiter.assigned";
    public static final String HIRING_MANAGER_ASSIGNED = "application.hiring-manager.assigned";
    public static final String OFFER_ACCEPTED = "application.offer.accepted";
    public static final String OFFER_REJECTED = "application.offer.rejected";

    // ---- Consumed -- owned/published by other (future) services ----
    public static final String RESUME_PARSED = "resume.resume.parsed";
    public static final String RESUME_MATCHED = "matching.resume.matched";
    public static final String CANDIDATE_SHORTLISTED = "shortlisting.candidate.shortlisted";
    public static final String INTERVIEW_COMPLETED = "interview.interview.completed";
    public static final String CODING_ASSESSMENT_COMPLETED = "assessment.coding.completed";
    public static final String BACKGROUND_VERIFICATION_COMPLETED = "verification.background.completed";
    public static final String OFFER_RELEASED = "offer.offer.released";
}
