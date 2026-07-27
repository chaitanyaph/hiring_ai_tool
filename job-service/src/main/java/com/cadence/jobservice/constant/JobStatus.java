package com.cadence.jobservice.constant;

import java.util.EnumSet;
import java.util.Set;

/**
 * Valid transitions are the actual business rule -- the UI's per-row
 * action set (Edit+Archive for Published, Edit+Publish for Draft,
 * Restore for Archived) is a direct reflection of this state machine,
 * not just a client-side convenience.
 */
public enum JobStatus {
    DRAFT,
    PUBLISHED,
    PAUSED,
    CLOSED,
    ARCHIVED,
    EXPIRED;

    public boolean canTransitionTo(JobStatus target) {
        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> EnumSet.of(PAUSED, CLOSED, ARCHIVED, EXPIRED).contains(target);
            case PAUSED -> EnumSet.of(PUBLISHED, CLOSED, ARCHIVED, EXPIRED).contains(target);
            case CLOSED -> target == ARCHIVED;
            case ARCHIVED -> target == DRAFT;
            case EXPIRED -> target == ARCHIVED;
        };
    }

    public static Set<JobStatus> activeCounted() {
        return EnumSet.of(DRAFT, PUBLISHED, PAUSED, CLOSED, EXPIRED);
    }
}
