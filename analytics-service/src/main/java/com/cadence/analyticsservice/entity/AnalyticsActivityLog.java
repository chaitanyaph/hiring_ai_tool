package com.cadence.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analytics_activity_log")
public class AnalyticsActivityLog {

    @Id
    @GeneratedValue
    private UUID id;

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
