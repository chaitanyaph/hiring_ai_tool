package com.cadence.applicationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Full audit trail of every Kafka event published or consumed for an application -- separate from status/stage history. */
@Entity
@Table(name = "application_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "direction", nullable = false, length = 10)
    private String direction;

    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void prePersist() {
        this.occurredAt = LocalDateTime.now();
    }
}
