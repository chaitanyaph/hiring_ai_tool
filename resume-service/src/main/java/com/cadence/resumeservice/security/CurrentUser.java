package com.cadence.resumeservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * The claims Resume Service actually needs out of the JWT. Every
 * candidate query is scoped to userId from here -- never a client-
 * supplied candidateId -- so one candidate can never read or delete
 * another's resumes. companyId (null for candidate tokens) is used to
 * scope a recruiter's preview/download access to candidates who
 * actually applied to their own company.
 */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final UUID companyId;
    private final String role;
}
