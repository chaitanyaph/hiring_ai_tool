package com.cadence.applicationservice.constant;

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
            // Trusted-network, service-to-service only (same trust model as
            // Company Service, which has no auth at all): called by the
            // future Resume Parsing/Matching, AI Interview and Coding
            // Assessment services to report scores back onto an
            // application. No end-user ever calls these directly.
            "/internal/**"
    };
}
