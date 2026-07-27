package com.cadence.applicationservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * The claims Application Service actually needs out of the JWT. Every
 * recruiter-side query is scoped to companyId from here -- never a
 * client-supplied companyId -- so one company can never see another's
 * applications no matter what the request path says. Candidate-side
 * queries are scoped to userId the same way.
 */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final UUID companyId;
    private final String role;
    private final Set<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
