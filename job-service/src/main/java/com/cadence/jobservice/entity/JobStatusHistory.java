package com.cadence.jobservice.entity;

import com.cadence.jobservice.constant.JobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobStatusHistory implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private JobStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private JobStatus toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 255)
    private String reason;

    @PrePersist
    void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}
