package com.cadence.candidateservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** 1:1 with Candidate -- wizard Step 9. Kept as its own row/table so it
 * can be PATCHed independently without touching the rest of the profile. */
@Entity
@Table(name = "candidate_job_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateJobPreference {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false, unique = true)
    private UUID candidateId;

    @Column(name = "preferred_work_type", length = 20)
    private String preferredWorkType;

    @Column(name = "preferred_employment_type", length = 20)
    private String preferredEmploymentType;

    @Column(name = "expected_salary", precision = 12, scale = 2)
    private BigDecimal expectedSalary;

    @Column(name = "salary_currency", nullable = false, length = 10)
    @Builder.Default
    private String salaryCurrency = "INR";

    @Column(name = "notice_period", length = 30)
    private String noticePeriod;

    @Column(name = "preferred_locations", length = 500)
    private String preferredLocations;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
