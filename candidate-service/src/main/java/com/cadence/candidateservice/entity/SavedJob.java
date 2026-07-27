package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJob {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void prePersist() {
        this.savedAt = LocalDateTime.now();
    }
}
