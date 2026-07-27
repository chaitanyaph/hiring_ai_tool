package com.cadence.applicationservice.entity;

import com.cadence.applicationservice.constant.ApplicationStage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_stage_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStageHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 30)
    private ApplicationStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 30)
    private ApplicationStage toStage;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "reason", length = 500)
    private String reason;

    @PrePersist
    protected void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}
