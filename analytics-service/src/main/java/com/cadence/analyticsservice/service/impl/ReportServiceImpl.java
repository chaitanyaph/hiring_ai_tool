package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.service.FunnelService;
import com.cadence.analyticsservice.service.RecruiterPerformanceService;
import com.cadence.analyticsservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private static final List<MetricKey> REPORT_KPI_KEYS = List.of(
            MetricKey.TOTAL_APPLICATIONS, MetricKey.HIRES,
            MetricKey.OFFERS_SENT, MetricKey.OFFERS_ACCEPTED, MetricKey.OFFERS_REJECTED);

    private final MetricSnapshotRepository metricSnapshotRepository;
    private final FunnelService funnelService;
    private final RecruiterPerformanceService recruiterPerformanceService;

    @Override
    public ReportResponse getDailyReport(UUID companyId, LocalDate date) {
        return buildReport("DAILY", date.toString(), companyId, null);
    }

    @Override
    public ReportResponse getMonthlyReport(UUID companyId, LocalDate month) {
        LocalDate monthBucket = month.withDayOfMonth(1);
        return buildReport("MONTHLY", MONTH_LABEL_FORMATTER.format(monthBucket), companyId, monthBucket);
    }

    @Override
    public ReportResponse getYearlyReport(UUID companyId, int year) {
        return buildReport("YEARLY", String.valueOf(year), companyId, null);
    }

    private ReportResponse buildReport(String reportType, String periodLabel, UUID companyId, LocalDate monthBucket) {
        MetricScope scope = companyId != null ? MetricScope.COMPANY : MetricScope.GLOBAL;
        UUID scopeId = companyId != null ? companyId : MetricScope.NO_SCOPE_ID;

        Map<MetricKey, Long> kpis = metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(
                        scope, scopeId, REPORT_KPI_KEYS, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE)
                .stream()
                .collect(Collectors.toMap(MetricSnapshot::getMetricKey, s -> s.getMetricValue().longValue()));

        long totalApplications = kpis.getOrDefault(MetricKey.TOTAL_APPLICATIONS, 0L);
        long totalHires = kpis.getOrDefault(MetricKey.HIRES, 0L);
        long offersSent = kpis.getOrDefault(MetricKey.OFFERS_SENT, 0L);
        long offersAccepted = kpis.getOrDefault(MetricKey.OFFERS_ACCEPTED, 0L);
        long offersRejected = kpis.getOrDefault(MetricKey.OFFERS_REJECTED, 0L);

        List<FunnelStageResponse> funnel = companyId != null
                ? funnelService.getCompanyFunnel(companyId).getStages()
                : funnelService.getGlobalFunnel().getStages();

        // Only the MONTHLY report carries a genuinely period-sliced series, and only when
        // company-scoped -- MONTHLY HIRES rows are never written at GLOBAL scope (see
        // MetricIngestionServiceImpl#onApplicationStatusChanged). DAILY/YEARLY have no
        // matching granularity anywhere in the ingestion pipeline.
        List<MonthlyPointResponse> hiringTrend = (monthBucket != null && companyId != null)
                ? buildMonthlyHiringTrend(companyId, monthBucket)
                : List.of();

        List<RecruiterPerformanceResponse> recruiterPerformance = companyId != null
                ? recruiterPerformanceService.getRecruiterPerformance(companyId)
                : List.of();

        return ReportResponse.builder()
                .reportType(reportType)
                .periodLabel(periodLabel)
                .generatedAt(LocalDateTime.now())
                .totalApplications(totalApplications)
                .totalHires(totalHires)
                .offersSent(offersSent)
                .offersAccepted(offersAccepted)
                .offersRejected(offersRejected)
                .offerAcceptanceRatePercent(percentOf(offersAccepted, offersAccepted + offersRejected))
                .funnel(funnel)
                .hiringTrend(hiringTrend)
                .recruiterPerformance(recruiterPerformance)
                .build();
    }

    private List<MonthlyPointResponse> buildMonthlyHiringTrend(UUID companyId, LocalDate monthBucket) {
        long value = metricSnapshotRepository
                .findByScopeAndScopeIdAndMetricKeyAndDimensionAndPeriodTypeAndPeriodDate(
                        MetricScope.COMPANY, companyId, MetricKey.HIRES, "", PeriodType.MONTHLY, monthBucket)
                .map(s -> s.getMetricValue().longValue())
                .orElse(0L);
        return List.of(MonthlyPointResponse.builder()
                .monthLabel(MONTH_LABEL_FORMATTER.format(monthBucket))
                .value(value)
                .build());
    }

    private Double percentOf(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return Math.round(part * 100.0 / whole * 100.0) / 100.0;
    }
}
