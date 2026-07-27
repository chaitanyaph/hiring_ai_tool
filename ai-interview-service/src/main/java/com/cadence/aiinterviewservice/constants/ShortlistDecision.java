package com.cadence.aiinterviewservice.constants;

/** score>=autoShortlistMinScore -> SHORTLISTED, score<autoRejectMaxScore -> REJECTED, else MANUAL_REVIEW. */
public enum ShortlistDecision {
    SHORTLISTED,
    REJECTED,
    MANUAL_REVIEW
}
