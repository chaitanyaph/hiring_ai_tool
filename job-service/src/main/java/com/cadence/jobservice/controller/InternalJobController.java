package com.cadence.jobservice.controller;

import com.cadence.jobservice.dto.response.ApiResponse;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * True machine-to-machine endpoint -- trusted network only, permitAll
 * (see SecurityConstants.PUBLIC_ENDPOINTS), same trust model as every
 * other service's /internal API. Built for Resume Parser Service to
 * fetch job requirements (skills, experience range, education) for
 * resume-to-job matching, without a human JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/jobs")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Job detail lookup -- trusted network only, no company scoping")
public class InternalJobController {

    private final JobService jobService;

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job detail (title, requirements) for cross-service matching")
    public ResponseEntity<ApiResponse<JobDetailResponse>> getJobDetail(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.getJobDetailInternal(jobId)));
    }
}
