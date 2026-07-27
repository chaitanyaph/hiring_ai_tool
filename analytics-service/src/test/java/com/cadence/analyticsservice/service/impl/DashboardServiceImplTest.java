package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.DashboardResponse;
import com.cadence.analyticsservice.dto.response.FunnelResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.service.FunnelService;
import com.cadence.analyticsservice.service.RecruiterPerformanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;
    @Mock
    private RecruiterPerformanceService recruiterPerformanceService;
    @Mock
    private FunnelService funnelService;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(metricSnapshotRepository, recruiterPerformanceService, funnelService);
    }

    @Test
    void getCompanyDashboard_shouldAssembleKpisAndDerivedRates() {
        UUID companyId = UUID.randomUUID();

        List<MetricSnapshot> snapshots = List.of(
                snapshot(MetricKey.TOTAL_JOBS, 10),
                snapshot(MetricKey.PUBLISHED_JOBS, 8),
                snapshot(MetricKey.CLOSED_JOBS, 2),
                snapshot(MetricKey.CANDIDATES_REGISTERED, 50),
                snapshot(MetricKey.TOTAL_APPLICATIONS, 100),
                snapshot(MetricKey.OFFERS_SENT, 20),
                snapshot(MetricKey.OFFERS_ACCEPTED, 15),
                snapshot(MetricKey.OFFERS_REJECTED, 5),
                snapshot(MetricKey.HIRES, 15));

        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(
                eq(MetricScope.COMPANY), eq(companyId), anyList(), eq(PeriodType.ALL_TIME), eq(PeriodType.ALL_TIME_DATE)))
                .thenReturn(snapshots);
        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDateBetweenOrderByPeriodDateAsc(
                eq(MetricScope.COMPANY), eq(companyId), eq(MetricKey.HIRES), eq(PeriodType.MONTHLY), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(funnelService.getCompanyFunnel(companyId)).thenReturn(FunnelResponse.builder().scope("COMPANY").stages(List.of()).build());
        when(recruiterPerformanceService.getRecruiterPerformance(companyId)).thenReturn(List.of());

        DashboardResponse response = dashboardService.getCompanyDashboard(companyId);

        assertThat(response.getTotalJobs()).isEqualTo(10);
        assertThat(response.getPublishedJobs()).isEqualTo(8);
        assertThat(response.getTotalApplications()).isEqualTo(100);
        assertThat(response.getTotalHires()).isEqualTo(15);
        assertThat(response.getOfferAcceptanceRatePercent()).isEqualTo(75.0);
        assertThat(response.getCandidateDropoffRatePercent()).isEqualTo(85.0);
        assertThat(response.getTotalCompanies()).isNull();
        assertThat(response.getDiversityRatioPercent()).isNull();
        assertThat(response.getAvgTimeToHireDays()).isNull();
        assertThat(response.getMonthlyHiring()).hasSize(12);
        verify(recruiterPerformanceService).getRecruiterPerformance(companyId);
    }

    @Test
    void getExecutiveDashboard_shouldPopulateCompanyCountsAndOmitRecruiterPerformance() {
        List<MetricSnapshot> snapshots = List.of(
                snapshot(MetricKey.TOTAL_COMPANIES, 5),
                snapshot(MetricKey.ACTIVE_COMPANIES, 4));

        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(
                eq(MetricScope.GLOBAL), eq(MetricScope.NO_SCOPE_ID), anyList(), eq(PeriodType.ALL_TIME), eq(PeriodType.ALL_TIME_DATE)))
                .thenReturn(snapshots);
        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDateBetweenOrderByPeriodDateAsc(
                eq(MetricScope.GLOBAL), eq(MetricScope.NO_SCOPE_ID), eq(MetricKey.HIRES), eq(PeriodType.MONTHLY), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(funnelService.getGlobalFunnel()).thenReturn(FunnelResponse.builder().scope("GLOBAL").stages(List.of()).build());

        DashboardResponse response = dashboardService.getExecutiveDashboard();

        assertThat(response.getTotalCompanies()).isEqualTo(5L);
        assertThat(response.getActiveCompanies()).isEqualTo(4L);
        assertThat(response.getRecruiterPerformance()).isNull();
        verifyNoInteractions(recruiterPerformanceService);
    }

    private MetricSnapshot snapshot(MetricKey key, long value) {
        return MetricSnapshot.builder().metricKey(key).metricValue(BigDecimal.valueOf(value)).build();
    }
}
