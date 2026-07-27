package com.cadence.candidateservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * The claims Candidate Service actually needs out of the JWT: who's
 * calling and what role they hold. Every self-service query is scoped
 * to userId from here -- never a client-supplied candidateId -- so one
 * candidate can never read or modify another candidate's profile no
 * matter what the request path says. companyId is null for CANDIDATE
 * tokens and only present for recruiting-side roles, which use it to
 * scope the one non-self-service endpoint this service exposes
 * (advancing an application's pipeline stage) to their own company.
 */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final String role;
    private final UUID companyId;
}
