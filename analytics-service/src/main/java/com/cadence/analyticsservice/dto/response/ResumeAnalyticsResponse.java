package com.cadence.analyticsservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalyticsResponse {
    private long resumesParsed;
    private long parseSuccessCount;
    private long parseFailureCount;
    private Double avgMatchScore;
}
