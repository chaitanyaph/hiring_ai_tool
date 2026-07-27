package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.FunnelResponse;
import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunnelServiceImplTest {

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;

    private FunnelServiceImpl funnelService;

    @BeforeEach
    void setUp() {
        funnelService = new FunnelServiceImpl(metricSnapshotRepository);
    }

    @Test
    void getCompanyFunnel_shouldOrderStagesByCountDescendingAndComputeConversionRates() {
        UUID companyId = UUID.randomUUID();
        List<MetricSnapshot> rows = List.of(
                stageSnapshot("APPLIED", 100),
                stageSnapshot("SCREENING", 60),
                stageSnapshot("HIRED", 20));

        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDate(
                MetricScope.COMPANY, companyId, MetricKey.FUNNEL_STAGE, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE))
                .thenReturn(rows);

        FunnelResponse response = funnelService.getCompanyFunnel(companyId);

        assertThat(response.getScope()).isEqualTo("COMPANY");
        assertThat(response.getStages()).extracting(FunnelStageResponse::getStage)
                .containsExactly("APPLIED", "SCREENING", "HIRED");
        assertThat(response.getStages().get(0).getPercentOfFirstStage()).isEqualTo(100.0);
        assertThat(response.getStages().get(0).getConversionFromPreviousStage()).isEqualTo(100.0);
        assertThat(response.getStages().get(1).getPercentOfFirstStage()).isEqualTo(60.0);
        assertThat(response.getStages().get(1).getConversionFromPreviousStage()).isEqualTo(60.0);
        assertThat(response.getStages().get(2).getPercentOfFirstStage()).isEqualTo(20.0);
        assertThat(response.getStages().get(2).getConversionFromPreviousStage())
                .isEqualTo(Math.round(20.0 / 60.0 * 100.0 * 100.0) / 100.0);
    }

    @Test
    void getGlobalFunnel_withNoIngestedRows_shouldReturnEmptyStages() {
        when(metricSnapshotRepository.findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDate(
                MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID, MetricKey.FUNNEL_STAGE, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE))
                .thenReturn(List.of());

        FunnelResponse response = funnelService.getGlobalFunnel();

        assertThat(response.getScope()).isEqualTo("GLOBAL");
        assertThat(response.getStages()).isEmpty();
    }

    private MetricSnapshot stageSnapshot(String stage, long count) {
        return MetricSnapshot.builder().dimension(stage).metricValue(BigDecimal.valueOf(count)).build();
    }
}
