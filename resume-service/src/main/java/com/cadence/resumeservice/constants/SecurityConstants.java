package com.cadence.resumeservice.constants;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Only the two true machine-to-machine endpoints are open --
     * metadata and raw MinIO object details, called by the future
     * Resume Parser Service. The recruiter-facing preview/download
     * endpoints also live under /api/v1/internal/resumes/** (per the
     * product spec's own path choice) but are NOT here -- they require
     * a real JWT + recruiting role, enforced via @PreAuthorize instead,
     * since a human recruiter (not a trusted service) is calling them.
     */
    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/internal/resumes/*",
            "/api/v1/internal/resumes/*/object"
    };
}
