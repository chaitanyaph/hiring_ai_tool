package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A reusable starting point ("Templates" button on the Jobs screen) --
 * stores a snapshot of description/requirements/pipeline as JSON rather
 * than a parallel normalized schema, since a template is just data to
 * pre-fill the wizard with, never queried or joined on its own fields.
 */
@Entity
@Table(name = "job_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplate implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "template_name", nullable = false, length = 150)
    private String templateName;

    @Lob
    @Column(name = "template_data_json", nullable = false, columnDefinition = "LONGTEXT")
    private String templateDataJson;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
