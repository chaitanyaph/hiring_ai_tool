package com.cadence.resumeservice.controller;

import com.cadence.resumeservice.dto.response.ApiResponse;
import com.cadence.resumeservice.dto.response.ResumeObjectDetailsResponse;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.service.ResumeService;
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
 * True machine-to-machine endpoints -- trusted network only, permitAll
 * (see SecurityConstants.PUBLIC_ENDPOINTS), same trust model as Company
 * Service. Called by the future Resume Parser Service: metadata first,
 * then /object to read the file straight out of MinIO with its own
 * credentials instead of proxying every download through this
 * service's HTTP path.
 */
@RestController
@RequestMapping("/api/v1/internal/resumes")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Resume metadata and storage-object lookups -- trusted network only")
public class InternalResumeController {

    private final ResumeService resumeService;

    @GetMapping("/{resumeId}")
    @Operation(summary = "Get resume metadata")
    public ResponseEntity<ApiResponse<ResumeResponse>> getMetadata(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeService.getMetadataInternal(resumeId)));
    }

    @GetMapping("/{resumeId}/object")
    @Operation(summary = "Get the MinIO object details (bucket/object name, checksum) for direct storage access")
    public ResponseEntity<ApiResponse<ResumeObjectDetailsResponse>> getObjectDetails(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", resumeService.getObjectDetailsInternal(resumeId)));
    }
}
