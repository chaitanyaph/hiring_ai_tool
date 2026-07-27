package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "anti_cheat_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntiCheatLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_assessment_id", nullable = false)
    private UUID candidateAssessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private AntiCheatEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "metadata", length = 500)
    private String metadata;

    @PrePersist
    protected void prePersist() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}
