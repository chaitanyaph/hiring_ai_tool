package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.FunnelResponse;
import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.service.FunnelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Stage order is derived purely from ingested volume (largest first), not a hardcoded
 * ApplicationStatus enum -- this service doesn't own that state machine (it lives in
 * application-service) and only ever sees the raw toStatus string on the wire. In the
 * normal case a funnel naturally tapers, so count-descending reconstructs the expected
 * shape without assuming stage names or order that could drift from the owning service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunnelServiceImpl implements FunnelService {

    private final MetricSnapshotRepository metricSnapshotRepository;

    @Override
    public FunnelResponse getGlobalFunnel() {
        return FunnelResponse.builder()
                .scope("GLOBAL")
                .stages(buildStages(MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID))
                .build();
    }

    @Override
    public FunnelResponse getCompanyFunnel(UUID companyId) {
        return FunnelResponse.builder()
                .scope("COMPANY")
                .stages(buildStages(MetricScope.COMPANY, companyId))
                .build();
    }

    private List<FunnelStageResponse> buildStages(MetricScope scope, UUID scopeId) {
        List<MetricSnapshot> rows = metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDate(
                        scope, scopeId, MetricKey.FUNNEL_STAGE, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE);

        List<FunnelStageResponse> stages = rows.stream()
                .sorted(Comparator.comparingLong((MetricSnapshot s) -> s.getMetricValue().longValue()).reversed())
                .map(s -> FunnelStageResponse.builder()
                        .stage(s.getDimension())
                        .count(s.getMetricValue().longValue())
                        .build())
                .collect(Collectors.toList());

        if (stages.isEmpty()) {
            return stages;
        }

        long firstStageCount = stages.get(0).getCount();
        long previousCount = firstStageCount;
        for (int i = 0; i < stages.size(); i++) {
            FunnelStageResponse stage = stages.get(i);
            stage.setPercentOfFirstStage(percentOf(stage.getCount(), firstStageCount));
            stage.setConversionFromPreviousStage(i == 0 ? Double.valueOf(100.0) : percentOf(stage.getCount(), previousCount));
            previousCount = stage.getCount();
        }
        return stages;
    }

    private Double percentOf(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return Math.round(part * 100.0 / whole * 100.0) / 100.0;
    }
}
