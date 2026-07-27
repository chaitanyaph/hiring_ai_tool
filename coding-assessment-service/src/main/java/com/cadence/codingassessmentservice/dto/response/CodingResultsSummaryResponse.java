package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

/** Backs Tab 3's KPI row. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingResultsSummaryResponse {
    private long completedCount;
    private Double avgScore;
    private Integer highestScore;
    private String highestScoreCandidateName;
    private Integer lowestScore;
    private String lowestScoreCandidateName;
}
