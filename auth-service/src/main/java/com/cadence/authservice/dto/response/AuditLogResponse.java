package com.cadence.authservice.dto.response;

import com.cadence.authservice.constant.AuditEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private AuditEventType eventType;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;
}
