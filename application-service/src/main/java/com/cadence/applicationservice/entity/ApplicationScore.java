package com.cadence.applicationservice.entity;

import com.cadence.applicationservice.constant.ScoreType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only: every score ever reported, even superseded ones -- applications.* holds only the latest per type. */
@Entity
@Table(name = "application_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationScore {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_type", nullable = false, length = 20)
    private ScoreType scoreType;

    @Column(name = "score_value", nullable = false)
    private Integer scoreValue;

    @Column(name = "source", length = 60)
    private String source;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}
