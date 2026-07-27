package com.cadence.analyticsservice.repository;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, UUID> {

    /** The upsert lookup -- sentinel values on scopeId/dimension/periodDate mean this equality match always works, never NULL-pitfalled. */
    Optional<MetricSnapshot> findByScopeAndScopeIdAndMetricKeyAndDimensionAndPeriodTypeAndPeriodDate(
            MetricScope scope, UUID scopeId, MetricKey metricKey, String dimension, PeriodType periodType, LocalDate periodDate);

    /** All dimension rows for one key (e.g. every funnel stage, every source channel). */
    List<MetricSnapshot> findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDate(
            MetricScope scope, UUID scopeId, MetricKey metricKey, PeriodType periodType, LocalDate periodDate);

    /** Batch-fetch several KPI keys at once for a dashboard assembly. */
    List<MetricSnapshot> findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(
            MetricScope scope, UUID scopeId, List<MetricKey> metricKeys, PeriodType periodType, LocalDate periodDate);

    /** Monthly time series for one key across a date range (e.g. the "Monthly hiring" bar chart). */
    List<MetricSnapshot> findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDateBetweenOrderByPeriodDateAsc(
            MetricScope scope, UUID scopeId, MetricKey metricKey, PeriodType periodType, LocalDate from, LocalDate to);
}
