package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAudit implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String details;

    @PrePersist
    void prePersist() {
        this.performedAt = LocalDateTime.now();
    }
}
