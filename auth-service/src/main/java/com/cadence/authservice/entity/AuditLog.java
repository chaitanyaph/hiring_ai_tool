package com.cadence.authservice.entity;

import com.cadence.authservice.constant.AuditEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * metadata is stored as a raw JSON string (serialized by the service
 * layer via Jackson) rather than a typed column, so new event fields
 * never require a schema migration. The column itself is still `json`
 * (see Flyway script) so it remains queryable with MySQL's native JSON
 * functions for analytics.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private AuditEventType eventType;

    @Column(length = 500)
    private String description;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(columnDefinition = "json")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
