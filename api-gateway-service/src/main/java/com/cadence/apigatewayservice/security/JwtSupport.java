package com.cadence.apigatewayservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

/**
 * Verification only, same shared HS256 secret every downstream service already
 * validates against (see job-service's JwtTokenValidator, the pattern this mirrors).
 * The Gateway never issues tokens -- auth-service is the sole issuer.
 */
@Slf4j
@Component
public class JwtSupport {

    private final SecretKey signingKey;

    public JwtSupport(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected invalid JWT at the Gateway: {}", e.getMessage());
            return false;
        }
    }

    /** Roles arrive as a list (see auth-service's JwtTokenProvider); the first entry is
     * the primary role, matching how every downstream JwtAuthenticationFilter already
     * reads a single "role" out of this same claim shape. */
    public String primaryRole(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            return first == null ? null : first.toString().replaceFirst("^ROLE_", "");
        }
        return null;
    }
}
