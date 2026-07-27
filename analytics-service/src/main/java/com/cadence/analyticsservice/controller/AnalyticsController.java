package com.cadence.analyticsservice.controller;

import com.cadence.analyticsservice.dto.response.ApiResponse;
import com.cadence.analyticsservice.dto.response.AssessmentAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.CandidateAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.InterviewAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.JobAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.OfferAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.ResumeAnalyticsResponse;
import com.cadence.analyticsservice.security.CurrentUserProvider;
import com.cadence.analyticsservice.service.DomainAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backs the 6 domain-analytics widgets on sec-analytics. Job/candidate endpoints are
 * company-scoped to the caller. Resume/interview/assessment/offer endpoints are always
 * platform-wide -- see DomainAnalyticsService javadoc for why (those source events never
 * carry companyId).
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Domain Analytics", description = "Job/candidate/resume/interview/assessment/offer analytics widgets")
public class AnalyticsController {

    private final DomainAnalyticsService domainAnalyticsService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/jobs")
    @Operation(summary = "Job analytics for the caller's company")
    public ResponseEntity<ApiResponse<JobAnalyticsResponse>> jobs() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getJobAnalytics(companyId)));
    }

    @GetMapping("/candidates")
    @Operation(summary = "Candidate analytics for the caller's company")
    public ResponseEntity<ApiResponse<CandidateAnalyticsResponse>> candidates() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getCandidateAnalytics(companyId)));
    }

    @GetMapping("/resumes")
    @Operation(summary = "Platform-wide resume parsing/matching analytics")
    public ResponseEntity<ApiResponse<ResumeAnalyticsResponse>> resumes() {
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getResumeAnalytics()));
    }

    @GetMapping("/interviews")
    @Operation(summary = "Platform-wide interview analytics")
    public ResponseEntity<ApiResponse<InterviewAnalyticsResponse>> interviews() {
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getInterviewAnalytics()));
    }

    @GetMapping("/assessments")
    @Operation(summary = "Platform-wide coding assessment analytics")
    public ResponseEntity<ApiResponse<AssessmentAnalyticsResponse>> assessments() {
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getAssessmentAnalytics()));
    }

    @GetMapping("/offers")
    @Operation(summary = "Platform-wide offer analytics")
    public ResponseEntity<ApiResponse<OfferAnalyticsResponse>> offers() {
        return ResponseEntity.ok(ApiResponse.ok("OK", domainAnalyticsService.getOfferAnalytics()));
    }
}
