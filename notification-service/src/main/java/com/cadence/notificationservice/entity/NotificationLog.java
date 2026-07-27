package com.cadence.notificationservice.entity;

import com.cadence.notificationservice.constants.LogLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only raw log lines -- backs the Figma's "Notification logs" tab. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 10)
    @Builder.Default
    private LogLevel level = LogLevel.INFO;

    @Column(name = "source", nullable = false, length = 60)
    private String source;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
