package com.cadence.jobservice.entity;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.constant.WorkType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLRestriction("is_deleted = false")
public class Job extends BaseAuditEntity implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "job_code", nullable = false, length = 40)
    private String jobCode;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", length = 20)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Column(name = "number_of_openings")
    private Integer numberOfOpenings;

    @Column(name = "application_deadline")
    private LocalDate applicationDeadline;

    /**
     * Denormalized from job_assignment for fast search/filter/display on
     * the listing screen without a join on every query. job_assignment
     * remains the authoritative record (and audit trail) of who's
     * assigned and when -- these two columns just always mirror its
     * current state.
     */
    @Column(name = "recruiter_id")
    private UUID recruiterId;

    @Column(name = "hiring_manager_id")
    private UUID hiringManagerId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}
