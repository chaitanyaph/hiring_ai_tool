package com.cadence.authservice.service.impl;

import com.cadence.authservice.entity.RefreshToken;
import com.cadence.authservice.exception.InvalidTokenException;
import com.cadence.authservice.exception.TokenExpiredException;
import com.cadence.authservice.repository.RefreshTokenRepository;
import com.cadence.authservice.service.RefreshTokenService;
import com.cadence.authservice.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.remember-me-refresh-token-expiration-ms}")
    private long rememberMeExpirationMs;

    @Override
    @Transactional
    public String issueRefreshToken(UUID userId, boolean rememberMe, String deviceInfo, String ipAddress) {
        String rawToken = TokenGenerator.generateOpaqueToken();
        long expirationMs = rememberMe ? rememberMeExpirationMs : refreshTokenExpirationMs;

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(TokenGenerator.sha256(rawToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .rememberMe(rememberMe)
                .expiresAt(LocalDateTime.now().plusNanos(expirationMs * 1_000_000))
                .build();
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateAndGet(String rawToken) {
        String hash = TokenGenerator.sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (token.isRevoked()) {
            // Reuse of a revoked/rotated token is a strong signal of theft:
            // proactively revoke the entire session family for this user.
            log.warn("Revoked refresh token reuse detected for user {}", token.getUserId());
            revokeAllForUser(token.getUserId());
            throw new InvalidTokenException("Refresh token has been revoked. All sessions have been invalidated for your safety.");
        }
        if (token.isExpired()) {
            throw new TokenExpiredException("Refresh token has expired, please log in again");
        }
        return token;
    }

    @Override
    @Transactional
    public String rotate(RefreshToken oldToken, String deviceInfo, String ipAddress) {
        String newRawToken = TokenGenerator.generateOpaqueToken();
        long expirationMs = oldToken.isRememberMe() ? rememberMeExpirationMs : refreshTokenExpirationMs;

        RefreshToken newToken = RefreshToken.builder()
                .userId(oldToken.getUserId())
                .tokenHash(TokenGenerator.sha256(newRawToken))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .rememberMe(oldToken.isRememberMe())
                .expiresAt(LocalDateTime.now().plusNanos(expirationMs * 1_000_000))
                .build();
        refreshTokenRepository.save(newToken);

        oldToken.setRevoked(true);
        oldToken.setRevokedAt(LocalDateTime.now());
        oldToken.setReplacedBy(newToken.getId());
        refreshTokenRepository.save(oldToken);

        return newRawToken;
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        String hash = TokenGenerator.sha256(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
    }
}
