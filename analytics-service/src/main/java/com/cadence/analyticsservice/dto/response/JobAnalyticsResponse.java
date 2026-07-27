package com.cadence.analyticsservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAnalyticsResponse {
    private long totalJobs;
    private long publishedJobs;
    private long closedJobs;
}
