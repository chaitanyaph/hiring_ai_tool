package com.cadence.candidateservice.controller;

import com.cadence.candidateservice.dto.request.*;
import com.cadence.candidateservice.dto.response.ApiResponse;
import com.cadence.candidateservice.dto.response.CandidateProfileResponse;
import com.cadence.candidateservice.dto.response.ResumeUploadResponse;
import com.cadence.candidateservice.security.CurrentUserProvider;
import com.cadence.candidateservice.service.CandidateProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Every endpoint here is self-service: scoped to the caller's own
 * userId from the JWT (CurrentUserProvider), never a client-supplied
 * candidateId, so one candidate can never read or edit another's
 * profile through this API.
 */
@RestController
@RequestMapping("/api/v1/candidates/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Profile", description = "The 10-step profile wizard and read-only profile view")
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Get my full profile")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateProfileService.getProfile(currentUserProvider.getCurrentUser())));
    }

    @PutMapping("/basic-info")
    @Operation(summary = "Wizard Step 1 -- basic info", description = "Also creates the candidate profile on first call")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateBasicInfo(@Valid @RequestBody BasicInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Basic info saved", candidateProfileService.updateBasicInfo(currentUserProvider.getCurrentUser(), request)));
    }

    @PostMapping(value = "/resume", consumes = "multipart/form-data")
    @Operation(summary = "Wizard Step 2 -- upload resume")
    public ResponseEntity<ApiResponse<ResumeUploadResponse>> uploadResume(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok("Resume uploaded", candidateProfileService.uploadResume(currentUserProvider.getCurrentUser(), file)));
    }

    @PutMapping("/education")
    @Operation(summary = "Wizard Step 3 -- education", description = "Full replace: supports add/edit/delete/reorder in one call")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateEducation(@Valid @RequestBody UpdateEducationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Education updated", candidateProfileService.updateEducation(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/experience")
    @Operation(summary = "Wizard Step 4 -- experience", description = "Full replace: supports add/edit/delete/reorder in one call")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateExperience(@Valid @RequestBody UpdateExperienceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Experience updated", candidateProfileService.updateExperience(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/skills")
    @Operation(summary = "Wizard Step 5 -- skills", description = "Full replace of the candidate's skill list")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateSkills(@RequestBody UpdateSkillsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Skills updated", candidateProfileService.updateSkills(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/projects")
    @Operation(summary = "Wizard Step 6 -- projects", description = "Full replace: supports add/edit/delete/reorder in one call")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateProjects(@Valid @RequestBody UpdateProjectsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Projects updated", candidateProfileService.updateProjects(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/certifications")
    @Operation(summary = "Wizard Step 7 -- certifications", description = "Full replace: supports add/edit/delete/reorder in one call")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateCertifications(@Valid @RequestBody UpdateCertificationsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Certifications updated", candidateProfileService.updateCertifications(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/languages")
    @Operation(summary = "Wizard Step 8 -- languages", description = "Full replace of the candidate's language list")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateLanguages(@RequestBody UpdateLanguagesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Languages updated", candidateProfileService.updateLanguages(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/job-preferences")
    @Operation(summary = "Wizard Step 9 -- job preferences")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updateJobPreferences(@RequestBody JobPreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job preferences updated", candidateProfileService.updateJobPreferences(currentUserProvider.getCurrentUser(), request)));
    }

    @PutMapping("/portfolio")
    @Operation(summary = "Wizard Step 10 -- portfolio links")
    public ResponseEntity<ApiResponse<CandidateProfileResponse>> updatePortfolio(@RequestBody PortfolioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Portfolio updated", candidateProfileService.updatePortfolio(currentUserProvider.getCurrentUser(), request)));
    }
}
