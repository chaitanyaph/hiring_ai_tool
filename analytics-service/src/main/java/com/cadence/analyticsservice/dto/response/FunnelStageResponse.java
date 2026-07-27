package com.cadence.analyticsservice.dto.response;

import lombok.*;

/** Mirrors the Figma's analyticsData[job].funnel tuple shape exactly: [stageLabel, count, percentOfFirst, conversionFromPrevious]. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FunnelStageResponse {
    private String stage;
    private long count;
    private Double percentOfFirstStage;
    private Double conversionFromPreviousStage;
}
