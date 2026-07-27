package com.cadence.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Kept as its own table rather than folded into metric_snapshot -- the Figma's "Recruiter performance" table is queried as multiple recruiters x multiple columns at once. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "recruiter_performance_snapshot")
public class RecruiterPerformanceSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "recruiter_id", nullable = false)
    private UUID recruiterId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "period_date", nullable = false)
    @Builder.Default
    private LocalDate periodDate = com.cadence.analyticsservice.constants.PeriodType.ALL_TIME_DATE;

    @Column(name = "open_reqs", nullable = false)
    @Builder.Default
    private int openReqs = 0;

    @Column(name = "applications_reviewed", nullable = false)
    @Builder.Default
    private int applicationsReviewed = 0;

    @Column(name = "hires_count", nullable = false)
    @Builder.Default
    private int hiresCount = 0;

    @Column(name = "avg_time_to_hire_days", precision = 6, scale = 2)
    private BigDecimal avgTimeToHireDays;

    @Column(name = "avg_interview_rating", precision = 4, scale = 2)
    private BigDecimal avgInterviewRating;

    @Column(name = "avg_offer_acceptance_pct", precision = 5, scale = 2)
    private BigDecimal avgOfferAcceptancePct;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
