package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.dto.response.AuditLogResponse;
import com.cadence.authservice.entity.AuditLog;
import com.cadence.authservice.mapper.AuditLogMapper;
import com.cadence.authservice.repository.AuditLogRepository;
import com.cadence.authservice.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Audit writes are @Async so a slow audit insert never adds latency to
 * the critical login/register path, and a failure here (logged, not
 * thrown) never breaks authentication itself -- security audit trail is
 * important but must not become a single point of failure for login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Async
    @Transactional
    public void record(UUID userId, AuditEventType eventType, String description, String ip, String userAgent) {
        record(userId, eventType, description, ip, userAgent, null);
    }

    @Override
    @Async
    @Transactional
    public void record(UUID userId, AuditEventType eventType, String description, String ip, String userAgent, Object metadata) {
        try {
            String metadataJson = metadata != null ? objectMapper.writeValueAsString(metadata) : null;
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .description(description)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .metadata(metadataJson)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log for event {}: {}", eventType, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsForUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(auditLogMapper::toResponse);
    }
}
