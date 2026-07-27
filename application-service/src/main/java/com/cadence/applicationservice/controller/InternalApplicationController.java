package com.cadence.applicationservice.controller;

import com.cadence.applicationservice.dto.request.ScoreUpdateRequest;
import com.cadence.applicationservice.dto.response.ApiResponse;
import com.cadence.applicationservice.dto.response.ApplicationResponse;
import com.cadence.applicationservice.service.ApplicationService;
import com.cadence.applicationservice.service.InternalScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Trusted-network only (see SecurityConstants.PUBLIC_ENDPOINTS -- no
 * bearer token required, same trust model as Company Service). Called
 * by the future Resume Matching, AI Interview and Coding Assessment
 * services as a synchronous alternative to the Kafka event path. These
 * only record a score value -- they never drive a status/stage
 * transition (see InternalScoreService Javadoc for why). The two GET
 * queries below are called by Resume Service to enforce its own
 * "can't delete a resume in use" and "recruiter must share a company
 * with an application from this candidate" rules.
 */
@RestController
@RequestMapping("/internal/application")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Score updates and lifecycle queries from other services -- trusted network only")
public class InternalApplicationController {

    private final InternalScoreService internalScoreService;
    private final ApplicationService applicationService;

    @PutMapping("/{id}/resume-score")
    @Operation(summary = "Record the resume match score")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateResumeScore(@PathVariable UUID id, @Valid @RequestBody ScoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Resume score recorded", internalScoreService.updateResumeScore(id, request)));
    }

    @PutMapping("/{id}/interview-score")
    @Operation(summary = "Record the AI interview score")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateInterviewScore(@PathVariable UUID id, @Valid @RequestBody ScoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Interview score recorded", internalScoreService.updateInterviewScore(id, request)));
    }

    @PutMapping("/{id}/coding-score")
    @Operation(summary = "Record the coding assessment score")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateCodingScore(@PathVariable UUID id, @Valid @RequestBody ScoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Coding score recorded", internalScoreService.updateCodingScore(id, request)));
    }

    @PutMapping("/{id}/overall-score")
    @Operation(summary = "Override the overall score", description = "Overall is normally the derived average of the 3 component scores")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateOverallScore(@PathVariable UUID id, @Valid @RequestBody ScoreUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Overall score recorded", internalScoreService.updateOverallScore(id, request)));
    }

    @GetMapping("/resume/{resumeId}/in-use")
    @Operation(summary = "Whether a resume is attached to any non-terminal application", description = "Called by Resume Service before allowing a delete")
    public ResponseEntity<ApiResponse<Boolean>> isResumeInUse(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.isResumeInUse(resumeId)));
    }

    @GetMapping("/exists")
    @Operation(summary = "Whether this candidate has ever applied to this company", description = "Called by Resume Service to scope a recruiter's resume preview/download access")
    public ResponseEntity<ApiResponse<Boolean>> hasApplicationFromCandidateToCompany(
            @RequestParam UUID candidateId, @RequestParam UUID companyId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.hasApplicationFromCandidateToCompany(candidateId, companyId)));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "List every application for a job", description = "Called by Resume Parser Service to build a per-job candidate ranking for resume matching")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsByJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.getApplicationsByJobInternal(jobId)));
    }
}
