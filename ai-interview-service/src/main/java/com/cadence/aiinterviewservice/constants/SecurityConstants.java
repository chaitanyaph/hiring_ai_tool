package com.cadence.aiinterviewservice.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Only the internal lookup is open -- a trusted-network, machine-
     * to-machine endpoint for a future Shortlisting/Notification
     * service to read shortlist/interview data without a JWT. Every
     * other endpoint requires a real bearer token issued by Auth Service.
     */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/internal/ai-interviews/*"
    };
}
