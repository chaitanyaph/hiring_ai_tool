package com.cadence.authservice.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-secret-key-that-is-at-least-32-bytes-long!".getBytes());

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, 900_000L, "cadence-auth-service-test");
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(
                userId, "user@email.com", Set.of("ROLE_CANDIDATE"), Set.of("CANDIDATE_APPLY"), null);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.isTokenValid(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(userId);

        List<String> roles = jwtTokenProvider.getRoles(token);
        assertThat(roles).contains("ROLE_CANDIDATE");
    }

    @Test
    void shouldEmbedClaims_correctly() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(
                userId, "recruiter@acme.com", Set.of("ROLE_HR_RECRUITER"), Set.of("JOB_CREATE"), companyId);

        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo("recruiter@acme.com");
        assertThat(claims.get("companyId")).isEqualTo(companyId.toString());
    }

    @Test
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId, "a@b.com", Set.of(), Set.of(), null);
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.isTokenValid(tampered)).isFalse();
    }

    @Test
    void shouldRejectGarbageToken() {
        assertThat(jwtTokenProvider.isTokenValid("not-a-jwt-at-all")).isFalse();
    }

    @Test
    void expirationSecondsShouldMatchConfiguredValue() {
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(900L);
    }
}
