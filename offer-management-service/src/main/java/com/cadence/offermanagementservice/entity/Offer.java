package com.cadence.offermanagementservice.entity;

import com.cadence.offermanagementservice.constants.ApprovalStatus;
import com.cadence.offermanagementservice.constants.DeclineReason;
import com.cadence.offermanagementservice.constants.EmploymentType;
import com.cadence.offermanagementservice.constants.OfferStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate root. Absorbs offer_status (enum), offer_salary +
 * offer_component (3 numeric columns + comma-separated benefits),
 * candidate_offer, joining_details, and approval_workflow (single-
 * stage approver/status/notes columns) -- see V1 migration header for
 * full consolidation reasoning.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "offer")
public class Offer extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "candidate_name", length = 200)
    private String candidateName;

    @Column(name = "candidate_email", length = 255)
    private String candidateEmail;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "base_salary", precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "variable_bonus", precision = 12, scale = 2)
    private BigDecimal variableBonus;

    @Column(name = "esop_equity", precision = 12, scale = 2)
    private BigDecimal esopEquity;

    @Column(name = "total_ctc", precision = 12, scale = 2)
    private BigDecimal totalCtc;

    @Column(name = "benefits", length = 300)
    private String benefits;

    @Column(name = "approver_id")
    private UUID approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", length = 500)
    private String approvalNotes;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decline_reason", length = 30)
    private DeclineReason declineReason;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "withdraw_reason", length = 500)
    private String withdrawReason;

    @Column(name = "created_by_recruiter_id")
    private UUID createdByRecruiterId;
}
