package com.cadence.candidateservice.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String PROFILE_CREATED = "candidate.profile.created";
    public static final String PROFILE_UPDATED = "candidate.profile.updated";
    public static final String RESUME_UPLOADED = "candidate.resume.uploaded";
    public static final String APPLICATION_SUBMITTED = "candidate.application.submitted";
    public static final String APPLICATION_WITHDRAWN = "candidate.application.withdrawn";
    public static final String APPLICATION_STAGE_CHANGED = "candidate.application.stage-changed";
    public static final String JOB_SAVED = "candidate.job.saved";
    public static final String JOB_UNSAVED = "candidate.job.unsaved";

    /** Published by auth-service on registration -- consumed here to pre-seed the candidate row (see kafka.consumer.AuthEventConsumer). */
    public static final String USER_REGISTERED = "auth.user.registered";
}
