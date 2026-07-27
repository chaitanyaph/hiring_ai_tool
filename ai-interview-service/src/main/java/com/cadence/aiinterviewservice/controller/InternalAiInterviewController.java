package com.cadence.aiinterviewservice.controller;

import com.cadence.aiinterviewservice.dto.response.ApiResponse;
import com.cadence.aiinterviewservice.dto.response.InterviewEvaluationReportResponse;
import com.cadence.aiinterviewservice.service.InterviewQueryService;
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
 * other service's /internal API. Built for the not-yet-built
 * Shortlisting/Notification services to read interview evaluation
 * data without a JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/ai-interviews")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "AI interview evaluation lookup -- trusted network only")
public class InternalAiInterviewController {

    private final InterviewQueryService interviewQueryService;

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get the full AI interview evaluation report for an application")
    public ResponseEntity<ApiResponse<InterviewEvaluationReportResponse>> getReport(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getReport(applicationId)));
    }
}
