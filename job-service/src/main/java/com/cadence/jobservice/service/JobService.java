package com.cadence.jobservice.service;

import com.cadence.jobservice.dto.request.*;
import com.cadence.jobservice.dto.response.DashboardResponse;
import com.cadence.jobservice.dto.response.JobCountsResponse;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobSummaryResponse;
import com.cadence.jobservice.dto.response.PagedResponse;
import com.cadence.jobservice.security.CurrentUser;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobService {

    // Wizard steps
    JobDetailResponse createDraft(JobBasicInfoRequest request, CurrentUser currentUser);
    JobDetailResponse updateBasicInfo(UUID jobId, JobBasicInfoRequest request, CurrentUser currentUser);
    JobDetailResponse updateRequirements(UUID jobId, JobRequirementsRequest request, CurrentUser currentUser);
    JobDetailResponse updatePipelineStages(UUID jobId, UpdatePipelineStagesRequest request, CurrentUser currentUser);
    JobDetailResponse getJobDetail(UUID jobId, CurrentUser currentUser);

    /** Trusted-network only, no company scoping -- see InternalJobController. */
    JobDetailResponse getJobDetailInternal(UUID jobId);

    // Lifecycle
    JobDetailResponse publishJob(UUID jobId, CurrentUser currentUser);
    JobDetailResponse pauseJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser);
    JobDetailResponse resumeJob(UUID jobId, CurrentUser currentUser);
    JobDetailResponse closeJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser);
    JobDetailResponse archiveJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser);
    JobDetailResponse restoreJob(UUID jobId, CurrentUser currentUser);
    void deleteJob(UUID jobId, CurrentUser currentUser);
    JobDetailResponse duplicateJob(UUID jobId, CurrentUser currentUser);

    // Assignment
    JobDetailResponse assignJob(UUID jobId, AssignJobRequest request, CurrentUser currentUser);

    // Listing / dashboard
    PagedResponse<JobSummaryResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable, CurrentUser currentUser);
    JobCountsResponse getCounts(CurrentUser currentUser);
    DashboardResponse getDashboard(CurrentUser currentUser);
}
