package com.cadence.resumeservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit + optimistic-locking columns shared by every entity in this
 * service. Deliberately has NO is_deleted/deletedAt/@SQLRestriction --
 * unlike the platform's other services, a resume's lifecycle is fully
 * expressed by its own `status` field (ACTIVE/DELETED/ARCHIVED), so a
 * second parallel "deleted" flag would just be redundant and confusing.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class BaseAuditEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
