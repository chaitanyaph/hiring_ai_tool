package com.cadence.codingassessmentservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Published by this service ----
    // EXACT existing topic string + shape already actively consumed by
    // application-service (ApplicationEventConsumer -> handleCodingAssessmentCompleted)
    // -- do not rename, this is load-bearing on the other side.
    public static final String CODING_ASSESSMENT_COMPLETED = "assessment.coding.completed";

    // Published the moment a candidate's invite row is actually created/refreshed
    // (real link + validity window) -- consumed by notification-service.
    public static final String CODING_ASSESSMENT_INVITED = "assessment.coding.invited";

    // Published by the reminder sweep -- consumed by notification-service.
    public static final String CODING_ASSESSMENT_REMINDER = "assessment.coding.reminder";

    // Forward-scaffolded (no consumer exists anywhere yet, same posture
    // every sibling service takes for events aimed at not-yet-built
    // Analytics/Notification services).
    public static final String ASSESSMENT_CREATED = "coding-assessment.assessment.created";
    public static final String ASSESSMENT_STARTED = "coding-assessment.assessment.started";
    public static final String CODE_SUBMITTED = "coding-assessment.submission.created";

    // ---- Consumed -- owned/published by AI Interview Service ----
    public static final String CANDIDATE_RECOMMENDED = "ai-interview.candidate.recommended";
}
