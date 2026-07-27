package com.cadence.resumeservice.controller;

import com.cadence.resumeservice.dto.request.RenameResumeRequest;
import com.cadence.resumeservice.dto.response.ApiResponse;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.security.CurrentUserProvider;
import com.cadence.resumeservice.service.ResumeContent;
import com.cadence.resumeservice.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Candidate self-service only -- upload/rename/set-default/delete are
 * exclusively here; recruiters can never reach these endpoints (see
 * RecruiterResumeController for their strictly preview/download-only access).
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Resumes", description = "Candidate resume upload, management, and self-service preview/download")
public class ResumeController {

    private final ResumeService resumeService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a resume", description = "Max 3 resumes, PDF only, max 5MB, unique checksum per candidate. First upload is automatically the default.")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName) {
        ResumeResponse response = resumeService.upload(currentUserProvider.getCurrentUser(), file, displayName);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Resume uploaded", response));
    }

    @GetMapping
    @Operation(summary = "Get my resumes")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> listMyResumes() {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeService.listMyResumes(currentUserProvider.getCurrentUser())));
    }

    @GetMapping("/{resumeId}")
    @Operation(summary = "Resume details")
    public ResponseEntity<ApiResponse<ResumeResponse>> getDetail(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeService.getDetail(currentUserProvider.getCurrentUser(), resumeId)));
    }

    @GetMapping("/{resumeId}/download")
    @Operation(summary = "Download my resume")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID resumeId) {
        return streamResponse(resumeService.downloadForCandidate(currentUserProvider.getCurrentUser(), resumeId), true);
    }

    @GetMapping("/{resumeId}/preview")
    @Operation(summary = "Preview my resume", description = "Inline rendering rather than a forced download")
    public ResponseEntity<InputStreamResource> preview(@PathVariable UUID resumeId) {
        return streamResponse(resumeService.previewForCandidate(currentUserProvider.getCurrentUser(), resumeId), false);
    }

    @PutMapping("/{resumeId}/default")
    @Operation(summary = "Set as default resume")
    public ResponseEntity<ApiResponse<ResumeResponse>> setDefault(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("Default resume updated", resumeService.setDefault(currentUserProvider.getCurrentUser(), resumeId)));
    }

    @PutMapping("/{resumeId}/rename")
    @Operation(summary = "Rename the resume's display name")
    public ResponseEntity<ApiResponse<ResumeResponse>> rename(@PathVariable UUID resumeId, @Valid @RequestBody RenameResumeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Resume renamed", resumeService.rename(currentUserProvider.getCurrentUser(), resumeId, request)));
    }

    @DeleteMapping("/{resumeId}")
    @Operation(summary = "Delete a resume", description = "Fails if the resume is attached to an active job application")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID resumeId) {
        resumeService.delete(currentUserProvider.getCurrentUser(), resumeId);
        return ResponseEntity.ok(ApiResponse.ok("Resume deleted"));
    }

    static ResponseEntity<InputStreamResource> streamResponse(ResumeContent content, boolean asAttachment) {
        ContentDisposition disposition = (asAttachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(content.fileName() != null ? content.fileName() : "resume.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.fileSize())
                .body(new InputStreamResource(content.content()));
    }
}
