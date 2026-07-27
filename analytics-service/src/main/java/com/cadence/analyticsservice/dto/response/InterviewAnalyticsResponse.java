package com.cadence.analyticsservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewAnalyticsResponse {
    private long interviewsCompleted;
    private long interviewsCancelled;
    private Double completionRatePercent;
    private Double avgAiInterviewScore;
    private Double avgTechnicalScore;
    private Double avgHrScore;
}
