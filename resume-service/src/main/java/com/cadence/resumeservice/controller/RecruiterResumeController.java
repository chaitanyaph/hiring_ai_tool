package com.cadence.resumeservice.controller;

import com.cadence.resumeservice.security.CurrentUserProvider;
import com.cadence.resumeservice.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Human, JWT-authenticated recruiter access -- despite living under
 * /api/v1/internal/resumes (the product spec's own path choice), this
 * is NOT the trusted-network/permitAll kind of "internal" (see
 * InternalResumeController for that). Recruiters can only preview and
 * download, never upload/edit/delete, and only for a candidate who has
 * actually applied to their own company (enforced in ResumeServiceImpl
 * .requireRecruiterAccess) -- ADMIN bypasses that company scoping.
 */
@RestController
@RequestMapping("/api/v1/internal/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Recruiter Resume Access", description = "Recruiter preview/download only, scoped to candidates who applied to their company")
public class RecruiterResumeController {

    private final ResumeService resumeService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{resumeId}/download")
    @Operation(summary = "Download a candidate's resume", description = "Only allowed if the candidate applied to the recruiter's own company")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID resumeId) {
        return ResumeController.streamResponse(resumeService.downloadForRecruiter(currentUserProvider.getCurrentUser(), resumeId), true);
    }

    @GetMapping("/{resumeId}/preview")
    @Operation(summary = "Preview a candidate's resume", description = "Only allowed if the candidate applied to the recruiter's own company")
    public ResponseEntity<InputStreamResource> preview(@PathVariable UUID resumeId) {
        return ResumeController.streamResponse(resumeService.previewForRecruiter(currentUserProvider.getCurrentUser(), resumeId), false);
    }
}
