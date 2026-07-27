package com.cadence.interviewmanagementservice.entity;

import com.cadence.interviewmanagementservice.constants.ActivityEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only audit trail -- carries the historical record that a separate interview_schedule/interview_status table would otherwise have needed. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "interview_activity_log")
public class InterviewActivityLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ActivityEventType eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(name = "details", length = 1000)
    private String details;
}
