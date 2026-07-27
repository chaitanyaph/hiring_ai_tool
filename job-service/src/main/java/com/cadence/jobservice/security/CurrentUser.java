package com.cadence.jobservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

/**
 * The claims Job Service actually needs out of the JWT: who's calling,
 * which company they belong to, and what they're allowed to do. Every
 * service-layer query is scoped to companyId from here -- never a
 * client-supplied companyId -- so one recruiter can never see another
 * company's jobs no matter what the request body/path says.
 */
@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final UUID companyId;
    private final String role;
    private final Set<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
