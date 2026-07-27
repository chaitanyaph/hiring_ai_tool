package com.cadence.resumeservice.constants;

/**
 * The lifecycle field for a resume row -- deliberately the *only*
 * "deleted" concept this service has (no separate is_deleted/deletedAt
 * flag): a resume is either usable (ACTIVE), soft-removed by the
 * candidate (DELETED), or superseded/kept for record only (ARCHIVED,
 * e.g. after CandidateDeleted cleanup). All read queries filter on
 * status explicitly rather than relying on a blanket @SQLRestriction.
 */
public enum ResumeStatus {
    ACTIVE,
    DELETED,
    ARCHIVED
}
