package com.cadence.authservice.service.impl;

import com.cadence.authservice.dto.response.SessionResponse;
import com.cadence.authservice.entity.UserSession;
import com.cadence.authservice.exception.ResourceNotFoundException;
import com.cadence.authservice.repository.UserSessionRepository;
import com.cadence.authservice.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepository userSessionRepository;

    @Override
    @Transactional
    public void createSession(UUID userId, UUID refreshTokenId, String deviceInfo, String ipAddress) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .refreshTokenId(refreshTokenId)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();
        userSessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(UUID userId, UUID currentRefreshTokenId) {
        return userSessionRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(s -> SessionResponse.builder()
                        .id(s.getId())
                        .deviceInfo(s.getDeviceInfo())
                        .ipAddress(s.getIpAddress())
                        .location(s.getLocation())
                        .active(s.isActive())
                        .current(currentRefreshTokenId != null && currentRefreshTokenId.equals(s.getRefreshTokenId()))
                        .lastActiveAt(s.getLastActiveAt())
                        .createdAt(s.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void endSession(UUID userId, UUID sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Session not found");
        }
        session.setActive(false);
        session.setEndedAt(LocalDateTime.now());
        userSessionRepository.save(session);
    }

    @Override
    @Transactional
    public void endAllSessions(UUID userId) {
        userSessionRepository.findAllByUserIdAndActiveTrue(userId).forEach(session -> {
            session.setActive(false);
            session.setEndedAt(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }
}
