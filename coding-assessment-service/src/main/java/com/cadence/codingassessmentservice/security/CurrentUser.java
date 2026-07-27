package com.cadence.codingassessmentservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/** The claims Coding Assessment Service actually needs out of the JWT. Every recruiter-facing query is scoped by companyId for multi-tenant filtering; candidate actions are attributed to userId. */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final UUID companyId;
    private final String role;
}
