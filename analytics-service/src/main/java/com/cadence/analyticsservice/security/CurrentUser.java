package com.cadence.analyticsservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CurrentUser {
    private final UUID userId;
    private final String email;
    private final UUID companyId;
    private final String role;
}
