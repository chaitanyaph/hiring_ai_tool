package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.DashboardResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.service.DashboardService;
import com.cadence.analyticsservice.service.FunnelService;
import com.cadence.analyticsservice.service.RecruiterPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int MONTHLY_HIRING_WINDOW_MONTHS = 12;
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private static final List<MetricKey> SCOPED_KPI_KEYS = List.of(
            MetricKey.TOTAL_JOBS, MetricKey.PUBLISHED_JOBS, MetricKey.CLOSED_JOBS,
            MetricKey.CANDIDATES_REGISTERED, MetricKey.TOTAL_APPLICATIONS,
            MetricKey.OFFERS_SENT, MetricKey.OFFERS_ACCEPTED, MetricKey.OFFERS_REJECTED, MetricKey.HIRES);

    private static final List<MetricKey> GLOBAL_ONLY_KPI_KEYS = List.of(
            MetricKey.TOTAL_COMPANIES, MetricKey.ACTIVE_COMPANIES);

    private final MetricSnapshotRepository metricSnapshotRepository;
    private final RecruiterPerformanceService recruiterPerformanceService;
    private final FunnelService funnelService;

    @Override
    public DashboardResponse getExecutiveDashboard() {
        return buildDashboard(MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID, true);
    }

    @Override
    public DashboardResponse getCompanyDashboard(UUID companyId) {
        return buildDashboard(MetricScope.COMPANY, companyId, false);
    }

    @Override
    public DashboardResponse getRecruiterDashboard(UUID companyId) {
        return buildDashboard(MetricScope.COMPANY, companyId, false);
    }

    @Override
    public DashboardResponse getHrDashboard(UUID companyId) {
        return buildDashboard(MetricScope.COMPANY, companyId, false);
    }

    @Override
    public DashboardResponse getHiringManagerDashboard(UUID companyId) {
        return buildDashboard(MetricScope.COMPANY, companyId, false);
    }

    private DashboardResponse buildDashboard(MetricScope scope, UUID scopeId, boolean executive) {
        Map<MetricKey, Long> kpis = fetchKpis(scope, scopeId, executive);

        long totalJobs = kpis.getOrDefault(MetricKey.TOTAL_JOBS, 0L);
        long publishedJobs = kpis.getOrDefault(MetricKey.PUBLISHED_JOBS, 0L);
        long closedJobs = kpis.getOrDefault(MetricKey.CLOSED_JOBS, 0L);
        long candidatesRegistered = kpis.getOrDefault(MetricKey.CANDIDATES_REGISTERED, 0L);
        long totalApplications = kpis.getOrDefault(MetricKey.TOTAL_APPLICATIONS, 0L);
        long offersSent = kpis.getOrDefault(MetricKey.OFFERS_SENT, 0L);
        long offersAccepted = kpis.getOrDefault(MetricKey.OFFERS_ACCEPTED, 0L);
        long offersRejected = kpis.getOrDefault(MetricKey.OFFERS_REJECTED, 0L);
        long totalHires = kpis.getOrDefault(MetricKey.HIRES, 0L);

        DashboardResponse.DashboardResponseBuilder builder = DashboardResponse.builder()
                .totalJobs(totalJobs)
                .publishedJobs(publishedJobs)
                .closedJobs(closedJobs)
                .candidatesRegistered(candidatesRegistered)
                .totalApplications(totalApplications)
                .offersSent(offersSent)
                .offersAccepted(offersAccepted)
                .offersRejected(offersRejected)
                .totalHires(totalHires)
                .offerAcceptanceRatePercent(percentOf(offersAccepted, offersAccepted + offersRejected))
                .candidateDropoffRatePercent(dropoffRate(totalApplications, totalHires))
                // No gender/diversity field exists on any event anywhere in the platform -- flagged, never fabricated.
                .diversityRatioPercent(null)
                // No first-applied->hired timestamp pair is ingested anywhere -- flagged, never fabricated.
                .avgTimeToHireDays(null)
                .funnel(executive ? funnelService.getGlobalFunnel().getStages() : funnelService.getCompanyFunnel(scopeId).getStages())
                .monthlyHiring(buildMonthlyHiring(scope, scopeId))
                .sourceBreakdown(List.of());

        if (executive) {
            builder.totalCompanies(kpis.getOrDefault(MetricKey.TOTAL_COMPANIES, 0L))
                    .activeCompanies(kpis.getOrDefault(MetricKey.ACTIVE_COMPANIES, 0L));
        } else {
            builder.recruiterPerformance(recruiterPerformanceService.getRecruiterPerformance(scopeId));
        }

        return builder.build();
    }

    private Map<MetricKey, Long> fetchKpis(MetricScope scope, UUID scopeId, boolean executive) {
        List<MetricKey> keys = new ArrayList<>(SCOPED_KPI_KEYS);
        if (executive) {
            keys.addAll(GLOBAL_ONLY_KPI_KEYS);
        }
        List<MetricSnapshot> snapshots = metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(
                        scope, scopeId, keys, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE);
        return snapshots.stream()
                .collect(Collectors.toMap(MetricSnapshot::getMetricKey, s -> s.getMetricValue().longValue()));
    }

    /**
     * MONTHLY-bucketed HIRES rows are only ever written at COMPANY scope (see
     * MetricIngestionServiceImpl#onApplicationStatusChanged) -- there is no GLOBAL monthly
     * aggregation in the ingestion pipeline, so the executive/GLOBAL dashboard's series is
     * always zero-filled. Flagged as a real gap rather than synthesizing a global rollup here.
     */
    private List<MonthlyPointResponse> buildMonthlyHiring(MetricScope scope, UUID scopeId) {
        LocalDate to = LocalDate.now().withDayOfMonth(1);
        LocalDate from = to.minusMonths(MONTHLY_HIRING_WINDOW_MONTHS - 1L);

        List<MetricSnapshot> rows = metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDateBetweenOrderByPeriodDateAsc(
                        scope, scopeId, MetricKey.HIRES, PeriodType.MONTHLY, from, to);
        Map<LocalDate, Long> byMonth = rows.stream()
                .collect(Collectors.toMap(MetricSnapshot::getPeriodDate, s -> s.getMetricValue().longValue()));

        List<MonthlyPointResponse> points = new ArrayList<>();
        for (LocalDate month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            points.add(MonthlyPointResponse.builder()
                    .monthLabel(MONTH_LABEL_FORMATTER.format(month))
                    .value(byMonth.getOrDefault(month, 0L))
                    .build());
        }
        return points;
    }

    private Double dropoffRate(long totalApplications, long totalHires) {
        if (totalApplications == 0) {
            return null;
        }
        return round2((1 - ((double) totalHires / totalApplications)) * 100.0);
    }

    private Double percentOf(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return round2(part * 100.0 / whole);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
