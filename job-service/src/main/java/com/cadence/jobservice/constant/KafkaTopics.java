package com.cadence.jobservice.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String JOB_CREATED = "job.job.created";
    public static final String JOB_UPDATED = "job.job.updated";
    public static final String JOB_PUBLISHED = "job.job.published";
    public static final String JOB_ARCHIVED = "job.job.archived";
    public static final String JOB_CLOSED = "job.job.closed";
    public static final String JOB_RESTORED = "job.job.restored";
    public static final String JOB_DELETED = "job.job.deleted";
}
