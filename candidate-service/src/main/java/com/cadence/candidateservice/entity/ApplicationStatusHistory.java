package com.cadence.candidateservice.entity;

import com.cadence.candidateservice.constant.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only audit trail of every stage transition on an application. */
@Entity
@Table(name = "application_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ApplicationStatus toStatus;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "note", length = 500)
    private String note;

    @PrePersist
    protected void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}
