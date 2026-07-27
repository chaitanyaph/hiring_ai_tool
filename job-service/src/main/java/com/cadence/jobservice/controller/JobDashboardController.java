package com.cadence.jobservice.controller;

import com.cadence.jobservice.dto.response.ApiResponse;
import com.cadence.jobservice.dto.response.DashboardResponse;
import com.cadence.jobservice.dto.response.JobCountsResponse;
import com.cadence.jobservice.security.CurrentUserProvider;
import com.cadence.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
@Tag(name = "Job Dashboard", description = "Listing header counts and dashboard aggregates")
public class JobDashboardController {

    private final JobService jobService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/counts")
    @Operation(summary = "Status counts for the listing header/filter tabs (All/Published/Draft/Archived)")
    public ResponseEntity<ApiResponse<JobCountsResponse>> counts() {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.getCounts(currentUserProvider.getCurrentUser())));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard aggregates -- totals, recently created, closing soon")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.getDashboard(currentUserProvider.getCurrentUser())));
    }
}
