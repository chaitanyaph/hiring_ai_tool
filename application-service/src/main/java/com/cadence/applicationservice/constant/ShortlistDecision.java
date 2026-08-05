package com.cadence.applicationservice.constant;

/** Mirrors ai-interview-service's own ShortlistDecision enum exactly -- this is the value carried on the CandidateShortlisted event this service consumes. */
public enum ShortlistDecision {
    SHORTLISTED,
    REJECTED,
    MANUAL_REVIEW
}
