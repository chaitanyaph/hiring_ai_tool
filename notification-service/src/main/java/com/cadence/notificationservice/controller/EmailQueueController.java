package com.cadence.notificationservice.controller;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.dto.request.RetryBulkRequest;
import com.cadence.notificationservice.dto.response.*;
import com.cadence.notificationservice.service.EmailQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Backs the Figma's Dashboard/Scheduled/Failed tabs (§A3). */
@RestController
@RequestMapping("/api/v1/email-queue")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Email Queue", description = "Queue/scheduled/failed views, retry, bulk retry, cancel, dashboard stats")
public class EmailQueueController {

    private final EmailQueueService emailQueueService;

    @GetMapping
    @Operation(summary = "List email queue items (optionally filtered by status/recipient) -- backs Scheduled and Failed tabs")
    public ResponseEntity<ApiResponse<PagedResponse<EmailQueueItemResponse>>> list(
            @RequestParam(required = false) EmailStatus status,
            @RequestParam(required = false) String recipient,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", emailQueueService.listEmails(recipient, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get email detail (drawer-email-history)")
    public ResponseEntity<ApiResponse<EmailQueueDetailResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", emailQueueService.getEmailDetail(id)));
    }

    @PostMapping("/retry/{id}")
    @Operation(summary = "Retry a single failed email")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable UUID id) {
        emailQueueService.retry(id);
        return ResponseEntity.ok(ApiResponse.ok("Retrying"));
    }

    @PostMapping("/retry-bulk")
    @Operation(summary = "Bulk retry selected failed emails (Failed tab's \"Retry selected\" bar)")
    public ResponseEntity<ApiResponse<Void>> retryBulk(@Valid @RequestBody RetryBulkRequest request) {
        emailQueueService.retryBulk(request);
        return ResponseEntity.ok(ApiResponse.ok("Retrying " + request.getEmailQueueIds().size() + " notifications"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a scheduled (not yet sent) email")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable UUID id) {
        emailQueueService.cancelScheduled(id);
        return ResponseEntity.ok(ApiResponse.ok("Scheduled notification cancelled"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard KPI stats (notif-dashboard tab)")
    public ResponseEntity<ApiResponse<EmailDashboardStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok("OK", emailQueueService.getDashboardStats()));
    }
}
