package com.cadence.resumeparserservice.dto.response;

import lombok.*;

/** Backs the Resume Analysis Dashboard's KPI row for a single job. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchSummaryResponse {
    private long totalCount;
    private long analyzedCount;
    private long awaitingCount;
    private long failedCount;
    private Double averageMatchScore;
    private Integer topMatchScore;
    private long belowThresholdCount;
}
