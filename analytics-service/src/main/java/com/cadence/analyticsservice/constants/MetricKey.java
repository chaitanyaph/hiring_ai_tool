package com.cadence.analyticsservice.constants;

/**
 * Every KPI, funnel-stage count, monthly-series value, and breakdown
 * row is stored as a row keyed by one of these -- see metric_snapshot
 * javadoc in V1 migration for the consolidation rationale. *_SUM/
 * *_COUNT pairs exist so averages are O(1) to read (SUM/COUNT) instead
 * of recomputed from raw event history.
 */
public enum MetricKey {
    TOTAL_COMPANIES,
    ACTIVE_COMPANIES,
    TOTAL_JOBS,
    PUBLISHED_JOBS,
    CLOSED_JOBS,
    CANDIDATES_REGISTERED,
    TOTAL_APPLICATIONS,
    FUNNEL_STAGE,
    HIRES,
    RESUMES_PARSED,
    RESUME_PARSE_SUCCESS,
    RESUME_PARSE_FAILURE,
    RESUME_SCORE_SUM,
    RESUME_SCORE_COUNT,
    AI_INTERVIEW_SCORE_SUM,
    AI_INTERVIEW_SCORE_COUNT,
    CANDIDATE_SHORTLISTED_COUNT,
    CODING_ASSESSMENT_SCORE_SUM,
    CODING_ASSESSMENT_SCORE_COUNT,
    CODING_ASSESSMENT_COMPLETED_COUNT,
    INTERVIEW_COMPLETED_COUNT,
    INTERVIEW_CANCELLED_COUNT,
    TECHNICAL_INTERVIEW_SCORE_SUM,
    TECHNICAL_INTERVIEW_SCORE_COUNT,
    HR_INTERVIEW_SCORE_SUM,
    HR_INTERVIEW_SCORE_COUNT,
    OFFERS_GENERATED,
    OFFERS_SENT,
    OFFERS_ACCEPTED,
    OFFERS_REJECTED,
    OFFERS_NEGOTIATION_REQUESTED,
    SOURCE_BREAKDOWN
}
