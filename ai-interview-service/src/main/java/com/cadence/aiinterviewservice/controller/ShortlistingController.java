package com.cadence.aiinterviewservice.controller;

import com.cadence.aiinterviewservice.dto.request.AssignRecruiterRequest;
import com.cadence.aiinterviewservice.dto.request.BulkApplicationIdsRequest;
import com.cadence.aiinterviewservice.dto.response.*;
import com.cadence.aiinterviewservice.service.ShortlistQueryService;
import com.cadence.aiinterviewservice.service.ShortlistingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs the AI Shortlisting screen: Shortlisted/Rejected/Manual-review tabs, the KPI row, the Ranking ("Top Candidates") table, and the manual-review queue's single/bulk actions. */
@RestController
@RequestMapping("/api/v1/ai-interviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "AI Shortlisting", description = "Threshold-based shortlist decisions, manual review queue, and candidate ranking")
public class ShortlistingController {

    private final ShortlistQueryService shortlistQueryService;
    private final ShortlistingService shortlistingService;

    @GetMapping("/shortlisted/{jobId}")
    @Operation(summary = "List shortlisted candidates for a job")
    public ResponseEntity<ApiResponse<PagedResponse<ShortlistItemResponse>>> getShortlisted(
            @PathVariable UUID jobId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", shortlistQueryService.getShortlisted(jobId, pageable)));
    }

    @GetMapping("/rejected/{jobId}")
    @Operation(summary = "List AI-rejected candidates for a job")
    public ResponseEntity<ApiResponse<PagedResponse<ShortlistItemResponse>>> getRejected(
            @PathVariable UUID jobId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", shortlistQueryService.getRejected(jobId, pageable)));
    }

    @GetMapping("/manual-review/{jobId}")
    @Operation(summary = "List candidates in the manual-review gray zone for a job")
    public ResponseEntity<ApiResponse<PagedResponse<ShortlistItemResponse>>> getManualReview(
            @PathVariable UUID jobId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", shortlistQueryService.getManualReview(jobId, pageable)));
    }

    @GetMapping("/shortlisting/summary/{jobId}")
    @Operation(summary = "Get the Shortlisting KPI row (Shortlisted / Rejected / Manual review / Auto-shortlist rate)")
    public ResponseEntity<ApiResponse<ShortlistSummaryResponse>> getSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", shortlistQueryService.getSummary(jobId)));
    }

    @GetMapping("/ranking/{jobId}")
    @Operation(summary = "Top-ranked (shortlisted) candidates for a job")
    public ResponseEntity<ApiResponse<List<ShortlistItemResponse>>> getRanking(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", shortlistQueryService.getRanking(jobId)));
    }

    @PostMapping("/{applicationId}/shortlist")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Manually shortlist one candidate from the manual-review queue")
    public ResponseEntity<ApiResponse<Void>> shortlistOne(@PathVariable UUID applicationId) {
        shortlistingService.shortlistOne(applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate shortlisted"));
    }

    @PostMapping("/{applicationId}/reject-manual")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Manually reject one candidate from the manual-review queue")
    public ResponseEntity<ApiResponse<Void>> rejectOne(@PathVariable UUID applicationId) {
        shortlistingService.rejectOne(applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate rejected"));
    }

    @PostMapping("/manual-review/bulk-shortlist")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Bulk-shortlist selected candidates from the manual-review queue")
    public ResponseEntity<ApiResponse<Void>> bulkShortlist(@Valid @RequestBody BulkApplicationIdsRequest request) {
        shortlistingService.bulkShortlist(request.getApplicationIds());
        return ResponseEntity.ok(ApiResponse.ok(request.getApplicationIds().size() + " candidate(s) shortlisted"));
    }

    @PostMapping("/manual-review/bulk-reject")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Bulk-reject selected candidates from the manual-review queue")
    public ResponseEntity<ApiResponse<Void>> bulkReject(@Valid @RequestBody BulkApplicationIdsRequest request) {
        shortlistingService.bulkReject(request.getApplicationIds());
        return ResponseEntity.ok(ApiResponse.ok(request.getApplicationIds().size() + " candidate(s) rejected"));
    }

    @PostMapping("/manual-review/assign-recruiter")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Assign a recruiter to selected candidates in the manual-review queue")
    public ResponseEntity<ApiResponse<Void>> assignRecruiter(@Valid @RequestBody AssignRecruiterRequest request) {
        shortlistingService.assignRecruiter(request.getApplicationIds(), request.getRecruiterId());
        return ResponseEntity.ok(ApiResponse.ok("Recruiter assigned"));
    }
}
