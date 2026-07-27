package com.cadence.applicationservice.entity;

import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.Priority;
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
 * The heart of the hiring workflow. company_id/job_id/candidate_id/
 * resume_id/assigned_recruiter_id/assigned_hiring_manager_id are plain
 * ids into other services' databases -- this service never joins
 * across them, only stores and displays the id (and, where useful,
 * denormalizes a display snapshot at write time, same pattern Job and
 * Candidate Service already use).
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

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "assigned_recruiter_id")
    private UUID assignedRecruiterId;

    @Column(name = "assigned_hiring_manager_id")
    private UUID assignedHiringManagerId;

    /** Snapshotted at apply() time via Feign, purely to support recruiter-side search without cross-service joins. */
    @Column(name = "candidate_name_snapshot", length = 150)
    private String candidateNameSnapshot;

    @Column(name = "candidate_email_snapshot", length = 150)
    private String candidateEmailSnapshot;

    @Column(name = "job_title_snapshot", length = 150)
    private String jobTitleSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    @Builder.Default
    private ApplicationStatus currentStatus = ApplicationStatus.APPLIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 30)
    @Builder.Default
    private ApplicationStage currentStage = ApplicationStage.APPLICATION;

    @Column(name = "resume_match_score")
    private Integer resumeMatchScore;

    @Column(name = "ai_interview_score")
    private Integer aiInterviewScore;

    @Column(name = "coding_score")
    private Integer codingScore;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "last_status_changed_at", nullable = false)
    private LocalDateTime lastStatusChangedAt;

    @PrePersist
    private void onApplyPersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.appliedAt == null) {
            this.appliedAt = now;
        }
        if (this.lastStatusChangedAt == null) {
            this.lastStatusChangedAt = now;
        }
    }
}
