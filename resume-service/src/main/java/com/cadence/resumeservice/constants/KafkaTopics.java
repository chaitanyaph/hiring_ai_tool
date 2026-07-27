package com.cadence.resumeservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Published by this service ----
    public static final String RESUME_UPLOADED = "resume.resume.uploaded";
    public static final String RESUME_DELETED = "resume.resume.deleted";
    public static final String RESUME_DEFAULT_CHANGED = "resume.default.changed";

    // ---- Consumed -- owned/published by Candidate Service ----
    public static final String CANDIDATE_DELETED = "candidate.profile.deleted";
}
