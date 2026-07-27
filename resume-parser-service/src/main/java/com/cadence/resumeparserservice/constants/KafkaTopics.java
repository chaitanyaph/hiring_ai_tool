package com.cadence.resumeparserservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Published by this service ----
    public static final String RESUME_PARSED = "resume-parser.resume.parsed";
    public static final String RESUME_PARSING_FAILED = "resume-parser.resume.parsing-failed";
    public static final String RESUME_ANALYZED = "resume-parser.resume.analyzed";
    public static final String RESUME_ANALYSIS_FAILED = "resume-parser.resume.analysis-failed";

    // ---- Consumed -- owned/published by Resume Service ----
    public static final String RESUME_UPLOADED = "resume.resume.uploaded";

    // ---- Consumed -- owned/published by Candidate Service ----
    public static final String CANDIDATE_DELETED = "candidate.profile.deleted";

    // ---- Consumed -- owned/published by Application Service ----
    // The actual trigger for match analysis (not RESUME_UPLOADED, which
    // has no job context) -- see README "Architecture Decisions".
    public static final String APPLICATION_CREATED = "application.application.created";
}
