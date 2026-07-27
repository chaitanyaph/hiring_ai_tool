package com.cadence.interviewmanagementservice.entity;

import com.cadence.interviewmanagementservice.constants.TimelineStage;
import com.cadence.interviewmanagementservice.constants.TimelineStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per (application_id, stage). Populated by consumed Kafka events (AI_INTERVIEW, CODING_ASSESSMENT) and this service's own scheduling actions. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "candidate_timeline")
public class CandidateTimeline {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    private TimelineStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TimelineStatus status = TimelineStatus.PENDING;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "score")
    private Integer score;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
