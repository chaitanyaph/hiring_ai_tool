package com.cadence.interviewmanagementservice.constants;

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
            "/api/v1/internal/interview-management/*"
    };
}
