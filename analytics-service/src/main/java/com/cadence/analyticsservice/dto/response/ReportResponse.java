package com.cadence.analyticsservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Backs /reports/daily, /monthly, /yearly and feeds the CSV/Excel/PDF export generators.
 * kpi/funnel/recruiterPerformance fields are as-of-generation cumulative snapshots, not
 * sliced to the requested period -- metric_snapshot only ever writes ALL_TIME rows for
 * these keys (plus MONTHLY rows for HIRES specifically); there is no per-day or per-year
 * bucketing anywhere in the ingestion pipeline (see MetricIngestionServiceImpl). hiringTrend
 * is the one genuinely period-aware field, and only for MONTHLY reports; it is left empty
 * for DAILY and YEARLY reports since no matching granularity is ingested -- flagged rather
 * than fabricated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private String reportType;
    private String periodLabel;
    private LocalDateTime generatedAt;
    private long totalApplications;
    private long totalHires;
    private long offersSent;
    private long offersAccepted;
    private long offersRejected;
    private Double offerAcceptanceRatePercent;
    private List<FunnelStageResponse> funnel;
    private List<MonthlyPointResponse> hiringTrend;
    private List<RecruiterPerformanceResponse> recruiterPerformance;
}
