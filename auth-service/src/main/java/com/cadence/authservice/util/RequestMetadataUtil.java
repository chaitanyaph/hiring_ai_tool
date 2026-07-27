package com.cadence.authservice.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestMetadataUtil {

    private RequestMetadataUtil() {}

    public static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static String extractUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}
