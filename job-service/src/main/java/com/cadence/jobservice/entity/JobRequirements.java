package com.cadence.jobservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequirements implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "min_experience_years")
    private Integer minExperienceYears;

    @Column(name = "max_experience_years")
    private Integer maxExperienceYears;

    @Column(length = 200)
    private String education;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String certifications;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String languages;

    @Column(name = "min_salary", precision = 12, scale = 2)
    private BigDecimal minSalary;

    @Column(name = "max_salary", precision = 12, scale = 2)
    private BigDecimal maxSalary;

    @Builder.Default
    @Column(name = "salary_currency", length = 10)
    private String salaryCurrency = "INR";

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String responsibilities;

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
