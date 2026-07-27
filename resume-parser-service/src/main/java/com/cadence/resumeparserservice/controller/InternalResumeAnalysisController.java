package com.cadence.resumeparserservice.controller;

import com.cadence.resumeparserservice.dto.response.ApiResponse;
import com.cadence.resumeparserservice.dto.response.PagedResponse;
import com.cadence.resumeparserservice.dto.response.ResumeMatchRankingItemResponse;
import com.cadence.resumeparserservice.dto.response.ResumeMatchResponse;
import com.cadence.resumeparserservice.service.ResumeAnalysisQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * True machine-to-machine endpoints -- trusted network only, permitAll
 * (see SecurityConstants.PUBLIC_ENDPOINTS), same trust model as
 * InternalParsedResumeController. Built for the not-yet-built
 * Shortlisting service to read match analysis data without a JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/resume-analysis")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Resume match analysis lookup -- trusted network only")
public class InternalResumeAnalysisController {

    private static final int MAX_JOB_RANKING_SIZE = 500;

    private final ResumeAnalysisQueryService resumeAnalysisQueryService;

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get the full match analysis for an application")
    public ResponseEntity<ApiResponse<ResumeMatchResponse>> getByApplication(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeAnalysisQueryService.getByApplicationId(applicationId)));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "Get every analyzed candidate ranking for a job")
    public ResponseEntity<ApiResponse<PagedResponse<ResumeMatchRankingItemResponse>>> getByJob(@PathVariable UUID jobId) {
        var page = resumeAnalysisQueryService.getRanking(jobId, null, PageRequest.of(0, MAX_JOB_RANKING_SIZE));
        return ResponseEntity.ok(ApiResponse.ok("OK", page));
    }
}
