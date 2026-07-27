package com.cadence.authservice.constant;

/**
 * Canonical role names. Kept as an enum (rather than free-text strings
 * scattered across the codebase) so role checks are compile-time safe;
 * the actual authorization source of truth still lives in the `roles`
 * table so new roles can be added by an admin without a code deploy.
 */
public enum RoleName {
    ROLE_ADMIN,
    ROLE_COMPANY_ADMIN,
    ROLE_HR_MANAGER,
    ROLE_HR_RECRUITER,
    ROLE_TECHNICAL_RECRUITER,
    ROLE_TALENT_ACQUISITION_MANAGER,
    ROLE_HIRING_MANAGER,
    ROLE_CANDIDATE
}
