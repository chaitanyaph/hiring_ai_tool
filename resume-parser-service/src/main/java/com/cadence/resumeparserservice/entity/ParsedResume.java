package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.AiProvider;
import com.cadence.resumeparserservice.constants.ParsingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per resume_id (unique) -- retries update this row in place
 * (incrementing attemptCount) rather than creating parse-attempt
 * history rows, since only the latest parse of a given resume is ever
 * shown anywhere in the product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "parsed_resume")
public class ParsedResume extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "resume_id", nullable = false, unique = true)
    private UUID resumeId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "checksum", nullable = false, columnDefinition = "CHAR(64)")
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ParsingStatus status = ParsingStatus.QUEUED;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_used", length = 20)
    private AiProvider providerUsed;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "location", length = 150)
    private String location;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(name = "portfolio_url", length = 300)
    private String portfolioUrl;

    @Column(name = "professional_summary", columnDefinition = "TEXT")
    private String professionalSummary;

    @Column(name = "current_company", length = 150)
    private String currentCompany;

    @Column(name = "current_designation", length = 150)
    private String currentDesignation;

    @Column(name = "total_experience_years", precision = 4, scale = 1)
    private BigDecimal totalExperienceYears;

    @Column(name = "notice_period", length = 50)
    private String noticePeriod;

    @Column(name = "expected_salary", length = 50)
    private String expectedSalary;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;
}
