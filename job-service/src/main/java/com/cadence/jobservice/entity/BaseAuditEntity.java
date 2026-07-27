package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Common audit, soft-delete and optimistic-locking columns shared by
 * every entity in this service. SQLRestriction on each subclass filters
 * out soft-deleted rows automatically on every query.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class BaseAuditEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private java.util.UUID createdBy;

    @Column(name = "updated_by")
    private java.util.UUID updatedBy;

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

    public void markDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
