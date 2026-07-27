package com.cadence.analyticsservice.entity;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
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

/**
 * Generic wide fact table -- every KPI, funnel-stage count, monthly
 * bar-chart value, and breakdown row is a differently-keyed row here.
 * scopeId/dimension/periodDate use sentinel values (never NULL) so a
 * single equality-based upsert lookup always works -- see V1
 * migration header.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "metric_snapshot")
public class MetricSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private MetricScope scope;

    @Column(name = "scope_id", nullable = false)
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_key", nullable = false, length = 50)
    private MetricKey metricKey;

    @Column(name = "dimension", nullable = false, length = 100)
    @Builder.Default
    private String dimension = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    @Builder.Default
    private PeriodType periodType = PeriodType.ALL_TIME;

    @Column(name = "period_date", nullable = false)
    @Builder.Default
    private LocalDate periodDate = PeriodType.ALL_TIME_DATE;

    @Column(name = "metric_value", nullable = false, precision = 20, scale = 4)
    @Builder.Default
    private BigDecimal metricValue = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
