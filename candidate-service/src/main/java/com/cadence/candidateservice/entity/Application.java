package com.cadence.candidateservice.entity;

import com.cadence.candidateservice.constant.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * job_title_snapshot/company_name_snapshot/location_snapshot/
 * employment_type_snapshot are captured once at apply() time via Feign
 * calls to Job/Company Service, so an application stays fully readable
 * even if the job is later archived/deleted upstream or those services
 * are temporarily unreachable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "applications")
@SQLRestriction("is_deleted = false")
public class Application extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "job_title_snapshot", length = 150)
    private String jobTitleSnapshot;

    @Column(name = "company_name_snapshot", length = 150)
    private String companyNameSnapshot;

    @Column(name = "location_snapshot", length = 200)
    private String locationSnapshot;

    @Column(name = "employment_type_snapshot", length = 20)
    private String employmentTypeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @PrePersist
    private void onApplyPersist() {
        if (this.appliedAt == null) {
            this.appliedAt = LocalDateTime.now();
        }
    }
}
