package com.cadence.apigatewayservice.constant;

import java.util.List;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String COMPANY_ID_HEADER = "X-Company-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    /**
     * Path prefixes (post-Gateway, pre-strip -- i.e. what the client actually calls)
     * that never require a JWT. Everything else is authenticated. auth-service owns
     * every one of these -- registration/login/refresh/password-reset/email-verify/
     * OAuth2 all have to work before a client has a token to send.
     */
    public static final List<String> PUBLIC_PATHS = List.of(
            "/auth/api/v1/auth/register",
            "/auth/api/v1/auth/login",
            "/auth/api/v1/auth/mfa/verify-login",
            "/auth/api/v1/auth/refresh-token",
            "/auth/api/v1/auth/forgot-password",
            "/auth/api/v1/auth/reset-password",
            "/auth/api/v1/auth/verify-email",
            "/auth/api/v1/auth/resend-verification",
            "/auth/oauth2/",
            "/actuator/"
    );
}
