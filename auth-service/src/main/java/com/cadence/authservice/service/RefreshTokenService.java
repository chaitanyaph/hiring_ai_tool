package com.cadence.authservice.service;

import com.cadence.authservice.entity.RefreshToken;

import java.util.UUID;

public interface RefreshTokenService {

    String issueRefreshToken(UUID userId, boolean rememberMe, String deviceInfo, String ipAddress);

    RefreshToken validateAndGet(String rawToken);

    String rotate(RefreshToken oldToken, String deviceInfo, String ipAddress);

    void revoke(String rawToken);

    void revokeAllForUser(UUID userId);
}
