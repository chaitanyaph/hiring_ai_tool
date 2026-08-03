package com.cadence.jobservice.controller;

import com.cadence.jobservice.constant.WorkType;
import com.cadence.jobservice.dto.response.ApiResponse;
import com.cadence.jobservice.dto.response.CandidateJobDetailResponse;
import com.cadence.jobservice.dto.response.CandidateJobSummaryResponse;
import com.cadence.jobservice.dto.response.PagedResponse;
import com.cadence.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Separate from JobController on purpose: that controller is
 * class-level restricted to recruiter/admin roles, and candidates must
 * never be able to reach any of its endpoints. This one exposes exactly
 * one read-only, published-jobs-only view, scoped to CANDIDATE.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Public Jobs", description = "Candidate-facing published job browsing across every company")
public class PublicJobController {

    private final JobService jobService;

    @GetMapping("/public")
    @Operation(summary = "Browse published jobs", description = "?title=&location=&workType=&page=&size=&sort=")
    public ResponseEntity<ApiResponse<PagedResponse<CandidateJobSummaryResponse>>> browse(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) WorkType workType,
            @PageableDefault(size = 100, sort = "publishedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.browsePublicJobs(title, location, workType, pageable)));
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get a single job's detail", description = "Any non-draft status -- Published/Paused/Closed/Archived/Expired all render with the matching status badge")
    public ResponseEntity<ApiResponse<CandidateJobDetailResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.getPublicJobDetail(id)));
    }
}
