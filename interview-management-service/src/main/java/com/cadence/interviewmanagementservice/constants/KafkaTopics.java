package com.cadence.interviewmanagementservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Consumed -- owned/published by ai-interview-service / coding-assessment-service ----
    public static final String CANDIDATE_RECOMMENDED = "ai-interview.candidate.recommended";
    public static final String CODING_ASSESSMENT_COMPLETED = "assessment.coding.completed";

    // ---- Published by this service, forward-scaffolded (no consumer exists anywhere yet) ----
    public static final String INTERVIEW_SCHEDULED = "interview-management.interview.scheduled";
    public static final String INTERVIEW_RESCHEDULED = "interview-management.interview.rescheduled";
    public static final String INTERVIEW_CANCELLED = "interview-management.interview.cancelled";
    public static final String INTERVIEW_COMPLETED = "interview-management.interview.completed";
    public static final String CANDIDATE_MOVED_TO_HR = "interview-management.candidate.moved-to-hr";
    public static final String CANDIDATE_REJECTED = "interview-management.candidate.rejected";
    public static final String CANDIDATE_SELECTED = "interview-management.candidate.selected";

    // EXACT existing topic string + shape already actively consumed by
    // application-service (ApplicationEventConsumer -> handleInterviewCompleted).
    // Its own InterviewCompletedEvent javadoc names this service as the
    // intended publisher for TECHNICAL/MANAGER/HR rounds -- do not rename,
    // this is load-bearing on the other side.
    public static final String APPLICATION_INTERVIEW_COMPLETED = "interview.interview.completed";
}
