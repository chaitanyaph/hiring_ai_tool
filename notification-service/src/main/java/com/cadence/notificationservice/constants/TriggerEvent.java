package com.cadence.notificationservice.constants;

/**
 * The Kafka-event-level trigger backing a template (notification_template.trigger_event).
 * NONE = confirmed no real publisher exists anywhere in the platform yet
 * (see README "Kafka Event Flow -- not wired") -- the template is
 * seeded and manageable, just not automatically dispatched.
 */
public enum TriggerEvent {
    TEAM_INVITATION_CREATED,
    USER_REGISTERED,
    PASSWORD_RESET_REQUESTED,
    APPLICATION_SUBMITTED,
    RESUME_UPLOADED,
    CANDIDATE_SHORTLISTED,
    AI_INTERVIEW_INVITED,
    CODING_ASSESSMENT_INVITED,
    INTERVIEW_SCHEDULED,
    INTERVIEW_RESCHEDULED,
    INTERVIEW_CANCELLED,
    OFFER_ACCEPTED,
    OFFER_REJECTED,
    NONE
}
