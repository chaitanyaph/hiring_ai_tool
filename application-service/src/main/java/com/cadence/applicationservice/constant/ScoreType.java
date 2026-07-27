package com.cadence.applicationservice.constant;

/** Backs the application_scores append-only log -- one row per score event received, ever. */
public enum ScoreType {
    RESUME_MATCH,
    AI_INTERVIEW,
    CODING,
    OVERALL
}
