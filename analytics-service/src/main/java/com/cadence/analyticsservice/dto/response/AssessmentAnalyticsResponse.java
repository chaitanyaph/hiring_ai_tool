package com.cadence.analyticsservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentAnalyticsResponse {
    private long assessmentsCompleted;
    private Double avgScore;
}
