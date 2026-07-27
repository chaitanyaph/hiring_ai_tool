package com.cadence.interviewmanagementservice.constants;

/**
 * Mirrors application-service's ApplicationStage values that this
 * service can actually observe. APPLICATION/AI_RESUME_SCREENING are
 * not populated by this service (no Feign client to resume-parser-
 * service or an events source exists in this service's scope) -- only
 * AI_INTERVIEW and CODING_ASSESSMENT are populated, from the two
 * consumed Kafka events, plus TECHNICAL_INTERVIEW/MANAGER_INTERVIEW/
 * HR_INTERVIEW from this service's own scheduling actions.
 */
public enum TimelineStage {
    AI_INTERVIEW,
    CODING_ASSESSMENT,
    TECHNICAL_INTERVIEW,
    MANAGER_INTERVIEW,
    HR_INTERVIEW
}
