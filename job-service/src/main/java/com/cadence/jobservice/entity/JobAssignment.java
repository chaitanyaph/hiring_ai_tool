package com.cadence.jobservice.entity;

import com.cadence.jobservice.constant.JobAssignmentRole;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAssignment implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_role", nullable = false, length = 20)
    private JobAssignmentRole assignmentRole;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    void prePersist() {
        this.assignedAt = LocalDateTime.now();
    }
}
