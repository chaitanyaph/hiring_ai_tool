package com.cadence.authservice.constant;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email/**",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/mfa/verify-login",
            "/oauth2/**",
            "/login/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    };
}
