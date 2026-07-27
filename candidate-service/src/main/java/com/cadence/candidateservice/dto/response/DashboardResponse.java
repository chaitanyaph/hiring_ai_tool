package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.util.List;

/**
 * profileViews and upcomingInterviewsCount are placeholders (0) until
 * Notification/Interview Scheduling services exist to feed them --
 * Candidate Service does not fabricate data for features it doesn't own.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Integer profileCompletionPercent;
    private Integer aiResumeScore;
    private long activeApplicationsCount;
    private long savedJobsCount;
    private long profileViews;
    private long upcomingInterviewsCount;
    private List<ApplicationResponse> recentApplications;
    private List<SavedJobResponse> savedJobs;
}
