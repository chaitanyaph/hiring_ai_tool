package com.cadence.authservice.service;

import com.cadence.authservice.dto.response.SessionResponse;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    void createSession(UUID userId, UUID refreshTokenId, String deviceInfo, String ipAddress);
    List<SessionResponse> getActiveSessions(UUID userId, UUID currentRefreshTokenId);
    void endSession(UUID userId, UUID sessionId);
    void endAllSessions(UUID userId);
}
