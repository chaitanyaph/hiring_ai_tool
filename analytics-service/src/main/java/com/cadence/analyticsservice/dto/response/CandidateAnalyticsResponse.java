package com.cadence.analyticsservice.dto.response;

import lombok.*;

/** experienceDistribution/educationDistribution from the text spec are omitted -- no candidate event anywhere carries experience/education fields; only resume-parser-service's per-job ranking endpoint has them, which doesn't fit this service's pre-aggregated model. Flagged in README. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateAnalyticsResponse {
    private long candidatesRegistered;
    private long totalApplications;
    private long shortlistedCount;
}
