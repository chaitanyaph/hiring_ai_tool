package com.cadence.authservice.service;

import com.cadence.authservice.constant.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cadence.authservice.dto.response.AuditLogResponse;

import java.util.UUID;

public interface AuditLogService {
    void record(UUID userId, AuditEventType eventType, String description, String ip, String userAgent);
    void record(UUID userId, AuditEventType eventType, String description, String ip, String userAgent, Object metadata);
    Page<AuditLogResponse> getLogsForUser(UUID userId, Pageable pageable);
}
