package com.cadence.notificationservice.constants;

/**
 * Only REAL, correctly-topic'd events researched directly from each
 * sibling service's own KafkaTopics.java are listed here (see README
 * "Kafka Event Flow" for the full real-vs-aspirational breakdown of
 * the originally-requested 28 event names). Topics that don't exist
 * anywhere are deliberately NOT subscribed -- a listener on a topic
 * with no publisher would silently sit empty forever.
 */
public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String COMPANY_CREATED = "company.company.created";
    public static final String TEAM_INVITATION_CREATED = "company.team-invitation.created";
    public static final String JOB_PUBLISHED = "job.job.published";
    public static final String JOB_CLOSED = "job.job.closed";
    public static final String USER_REGISTERED = "auth.user.registered";
    public static final String PASSWORD_RESET_REQUESTED = "auth.password.reset-requested";
    public static final String APPLICATION_CREATED = "application.application.created";
    public static final String RESUME_UPLOADED = "resume.resume.uploaded";
    public static final String RESUME_ANALYZED = "resume-parser.resume.analyzed";
    public static final String CANDIDATE_SHORTLISTED = "ai-interview.candidate.shortlisted";
    public static final String AI_INTERVIEW_INVITED = "ai-interview.interview.invited";
    public static final String AI_INTERVIEW_COMPLETED = "ai-interview.interview.completed";
    public static final String CODING_ASSESSMENT_INVITED = "assessment.coding.invited";
    public static final String CODING_ASSESSMENT_COMPLETED = "assessment.coding.completed";
    public static final String INTERVIEW_SCHEDULED = "interview-management.interview.scheduled";
    public static final String INTERVIEW_RESCHEDULED = "interview-management.interview.rescheduled";
    public static final String INTERVIEW_CANCELLED = "interview-management.interview.cancelled";
    public static final String CANDIDATE_MOVED_TO_HR = "interview-management.candidate.moved-to-hr";
    public static final String CANDIDATE_SELECTED = "interview-management.candidate.selected";
    public static final String CANDIDATE_REJECTED = "interview-management.candidate.rejected";
    public static final String OFFER_ACCEPTED = "application.offer.accepted";
    public static final String OFFER_REJECTED = "application.offer.rejected";
}
