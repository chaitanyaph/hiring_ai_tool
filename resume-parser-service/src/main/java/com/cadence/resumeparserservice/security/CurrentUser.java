package com.cadence.resumeparserservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * The claims Resume Parser Service actually needs out of the JWT.
 * Every recruiter-facing query is scoped by companyId for future
 * multi-tenant filtering; retry actions are attributed to userId via
 * parsed_resume.updatedBy.
 */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final UUID companyId;
    private final String role;
}
