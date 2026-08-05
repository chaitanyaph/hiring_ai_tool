package com.cadence.aiinterviewservice.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    // ---- Published by this service ----
    public static final String CANDIDATE_SHORTLISTED = "ai-interview.candidate.shortlisted";
    public static final String INTERVIEW_INVITED = "ai-interview.interview.invited";
    public static final String INTERVIEW_STARTED = "ai-interview.interview.started";
    public static final String INTERVIEW_COMPLETED = "ai-interview.interview.completed";
    public static final String INTERVIEW_EVALUATED = "ai-interview.interview.evaluated";
    public static final String CANDIDATE_RECOMMENDED = "ai-interview.candidate.recommended";

    // ---- Consumed -- owned/published by Resume Parser Service ----
    // The trigger for AI Shortlisting: a resume has been matched against a job.
    public static final String RESUME_ANALYZED = "resume-parser.resume.analyzed";
}
