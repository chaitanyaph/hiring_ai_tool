package com.cadence.analyticsservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Backs all 5 dashboard endpoints -- fields are nullable and each
 * DashboardService method only populates the ones relevant to that
 * role (§9 of the architecture doc: "each dashboard should expose
 * only the relevant analytics for that role" is achieved by leaving
 * irrelevant fields null, not by 5 separate DTO classes).
 * diversityRatioPercent is always null -- no gender/diversity field
 * exists on any event anywhere in the platform, flagged in README,
 * not fabricated to match the Figma's static 41% mockup value.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Long totalCompanies;
    private Long activeCompanies;
    private long totalJobs;
    private long publishedJobs;
    private long closedJobs;
    private long candidatesRegistered;
    private long totalApplications;
    private long offersSent;
    private long offersAccepted;
    private long offersRejected;
    private long totalHires;
    private Double offerAcceptanceRatePercent;
    private Double candidateDropoffRatePercent;
    private Double diversityRatioPercent;
    private Double avgTimeToHireDays;
    private List<FunnelStageResponse> funnel;
    private List<MonthlyPointResponse> monthlyHiring;
    private List<LabeledValueResponse> sourceBreakdown;
    private List<RecruiterPerformanceResponse> recruiterPerformance;
}
