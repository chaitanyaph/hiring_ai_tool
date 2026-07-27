package com.cadence.candidateservice.constant;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            // Trusted-network, service-to-service lookup (same trust model as
            // Company Service, which has no auth at all) -- returns only
            // non-sensitive fields (no email/phone) so other services (e.g.
            // Application Service) can validate "profile completed"/"resume
            // exists" without needing a candidate's own bearer token.
            "/api/v1/candidates/*/summary"
    };
}
