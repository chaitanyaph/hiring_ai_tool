package com.cadence.aiinterviewservice.constants;

/**
 * Automatic scoring is a strict binary rule: score &gt;= autoShortlistMinScore -> SHORTLISTED,
 * else -> REJECTED. MANUAL_REVIEW is kept as a valid value (recruiters can still see/assign it
 * on existing rows) but is no longer produced by ShortlistingServiceImpl.decisionFor().
 */
public enum ShortlistDecision {
    SHORTLISTED,
    REJECTED,
    MANUAL_REVIEW
}
