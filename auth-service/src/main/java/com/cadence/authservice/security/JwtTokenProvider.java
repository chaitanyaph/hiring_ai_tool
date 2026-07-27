package com.cadence.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Signs and verifies access tokens with HS256. The secret is injected
 * from config (backed by Vault/Config Server in real deployments, never
 * hardcoded) and must be >= 256 bits for HS256, enforced at startup.
 *
 * Access tokens are short-lived (15 min) and stateless -- they are never
 * persisted. Only refresh tokens touch the database, which keeps the
 * hot path (every authenticated request) fast and horizontally scalable
 * across Gateway instances without a shared session store.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.issuer = issuer;
    }

    public String generateAccessToken(UUID userId, String email, Set<String> roles, Set<String> permissions, UUID companyId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("email", email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("companyId", companyId != null ? companyId.toString() : null)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        return List.of();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationMs / 1000;
    }
}
