package com.cadence.codingassessmentservice.constants;

/** DRAFT questions are excluded from the assessment builder's picker; only ACTIVE questions can be attached to an assessment. */
public enum QuestionStatus {
    DRAFT,
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
