package com.cadence.candidateservice.controller;

import com.cadence.candidateservice.dto.request.SaveJobRequest;
import com.cadence.candidateservice.dto.response.ApiResponse;
import com.cadence.candidateservice.dto.response.SavedJobResponse;
import com.cadence.candidateservice.security.CurrentUserProvider;
import com.cadence.candidateservice.service.SavedJobService;
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
@RequestMapping("/api/v1/saved-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Saved Jobs", description = "Bookmark jobs to apply to later")
public class SavedJobController {

    private final SavedJobService savedJobService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Save a job", description = "Idempotent -- saving an already-saved job just returns the existing record")
    public ResponseEntity<ApiResponse<SavedJobResponse>> saveJob(@Valid @RequestBody SaveJobRequest request) {
        SavedJobResponse response = savedJobService.saveJob(currentUserProvider.getCurrentUser(), request.getJobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Job saved", response));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Unsave a job")
    public ResponseEntity<ApiResponse<Void>> unsaveJob(@PathVariable UUID jobId) {
        savedJobService.unsaveJob(currentUserProvider.getCurrentUser(), jobId);
        return ResponseEntity.ok(ApiResponse.ok("Job removed from saved list"));
    }

    @GetMapping
    @Operation(summary = "List my saved jobs")
    public ResponseEntity<ApiResponse<List<SavedJobResponse>>> listSavedJobs() {
        return ResponseEntity.ok(ApiResponse.ok("OK", savedJobService.listSavedJobs(currentUserProvider.getCurrentUser())));
    }
}
