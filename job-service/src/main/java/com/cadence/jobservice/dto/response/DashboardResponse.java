package com.cadence.jobservice.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private long totalJobs;
    private long publishedJobs;
    private long draftJobs;
    private long archivedJobs;
    private List<JobSummaryResponse> recentlyCreated;
    private List<JobSummaryResponse> closingSoon;

    /** Placeholder -- real number comes from the not-yet-built Candidate Service. */
    @Builder.Default
    private long applicationsCount = 0;
}
