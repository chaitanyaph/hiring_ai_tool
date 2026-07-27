package com.cadence.resumeparserservice.controller;

import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.service.ParsedResumeQueryService;
import com.cadence.resumeparserservice.service.ResumeParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs the drawer-parsing view: the full aggregate plus every sub-section, the stepper, the logs box, and the retry action. */
@RestController
@RequestMapping("/api/v1/parser/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Parsed Resume", description = "Full parsed-resume detail, its sub-sections, processing status, logs, and retry")
public class ParsedResumeController {

    private final ParsedResumeQueryService parsedResumeQueryService;
    private final ResumeParsingService resumeParsingService;

    @GetMapping("/{resumeId}")
    @Operation(summary = "Get the full parsed resume (drawer main view)")
    public ResponseEntity<ApiResponse<ParsedResumeResponse>> getAggregate(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getAggregate(resumeId)));
    }

    @GetMapping("/{resumeId}/skills")
    @Operation(summary = "Get extracted skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getSkills(resumeId)));
    }

    @GetMapping("/{resumeId}/experience")
    @Operation(summary = "Get extracted work experience")
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> getExperience(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getExperience(resumeId)));
    }

    @GetMapping("/{resumeId}/education")
    @Operation(summary = "Get extracted education")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> getEducation(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getEducation(resumeId)));
    }

    @GetMapping("/{resumeId}/projects")
    @Operation(summary = "Get extracted projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getProjects(resumeId)));
    }

    @GetMapping("/{resumeId}/certifications")
    @Operation(summary = "Get extracted certifications")
    public ResponseEntity<ApiResponse<List<CertificationResponse>>> getCertifications(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getCertifications(resumeId)));
    }

    @GetMapping("/{resumeId}/status")
    @Operation(summary = "Get the processing status (drawer's 4-step stepper)")
    public ResponseEntity<ApiResponse<ParsingStatusResponse>> getStatus(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getStatus(resumeId)));
    }

    @GetMapping("/{resumeId}/logs")
    @Operation(summary = "Get the parsing log trail")
    public ResponseEntity<ApiResponse<List<ParserLogResponse>>> getLogs(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getLogs(resumeId)));
    }

    @PostMapping("/{resumeId}/retry")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Retry a failed parse", description = "Only allowed when the resume's current status is FAILED")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable UUID resumeId) {
        resumeParsingService.retryParsing(resumeId);
        return ResponseEntity.accepted().body(ApiResponse.ok("Retry queued"));
    }
}
