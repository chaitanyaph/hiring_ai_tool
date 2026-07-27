package com.cadence.candidateservice.controller;

import com.cadence.candidateservice.dto.request.ApplyToJobRequest;
import com.cadence.candidateservice.dto.request.ChangeApplicationStageRequest;
import com.cadence.candidateservice.dto.response.ApiResponse;
import com.cadence.candidateservice.dto.response.ApplicationResponse;
import com.cadence.candidateservice.security.CurrentUserProvider;
import com.cadence.candidateservice.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Apply/withdraw/track the 9-stage application pipeline")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to a job", description = "Fails if already applied, or if the job is not currently PUBLISHED")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(@Valid @RequestBody ApplyToJobRequest request) {
        ApplicationResponse response = applicationService.apply(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Application submitted", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "List my applications", description = "?filter=all|active|offer|rejected (default all)")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listMyApplications(
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.listMyApplications(currentUserProvider.getCurrentUser(), filter)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get application detail with full stage history")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.getApplicationDetail(currentUserProvider.getCurrentUser(), id)));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Withdraw an application", description = "Only allowed while the application is not already in a terminal stage")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Application withdrawn", applicationService.withdraw(currentUserProvider.getCurrentUser(), id)));
    }

    @PostMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Advance an application's pipeline stage",
            description = "Recruiting-side roles only -- scoped to the caller's own company; validates the 9-stage state machine")
    public ResponseEntity<ApiResponse<ApplicationResponse>> changeStage(@PathVariable UUID id, @Valid @RequestBody ChangeApplicationStageRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Application stage updated", applicationService.changeStage(currentUserProvider.getCurrentUser(), id, request)));
    }
}
