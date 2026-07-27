package com.cadence.jobservice.controller;

import com.cadence.jobservice.dto.request.SaveTemplateRequest;
import com.cadence.jobservice.dto.response.ApiResponse;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobTemplateResponse;
import com.cadence.jobservice.security.CurrentUserProvider;
import com.cadence.jobservice.service.JobTemplateService;
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

/** Backs the "Templates" button on the Jobs listing screen. */
@RestController
@RequestMapping("/api/v1/job-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
@Tag(name = "Job Templates", description = "Reusable job configurations")
public class JobTemplateController {

    private final JobTemplateService jobTemplateService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/from-job/{jobId}")
    @Operation(summary = "Save an existing job's configuration as a reusable template")
    public ResponseEntity<ApiResponse<JobTemplateResponse>> saveAsTemplate(@PathVariable UUID jobId, @Valid @RequestBody SaveTemplateRequest request) {
        JobTemplateResponse response = jobTemplateService.saveAsTemplate(jobId, request, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Template saved", response));
    }

    @GetMapping
    @Operation(summary = "List templates for the current company")
    public ResponseEntity<ApiResponse<List<JobTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobTemplateService.listTemplates(currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{templateId}/create-draft")
    @Operation(summary = "Create a new job draft pre-filled from a template")
    public ResponseEntity<ApiResponse<JobDetailResponse>> createDraftFromTemplate(@PathVariable UUID templateId) {
        JobDetailResponse response = jobTemplateService.createDraftFromTemplate(templateId, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Job draft created from template", response));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "Delete a template")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID templateId) {
        jobTemplateService.deleteTemplate(templateId, currentUserProvider.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.ok("Template deleted"));
    }
}
