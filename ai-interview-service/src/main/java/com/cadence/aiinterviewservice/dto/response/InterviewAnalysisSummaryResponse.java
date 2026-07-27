package com.cadence.aiinterviewservice.dto.response;

import lombok.*;

/** Backs the AI Interviews "Analysis dashboard" KPI row. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAnalysisSummaryResponse {
    private long completedCount;
    private long completedThisWeekCount;
    private Double avgOverallScore;
    private Double avgCommunicationScore;
    private long flaggedForReviewCount;
}
