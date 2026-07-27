package com.cadence.notificationservice.controller;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.dto.response.ApiResponse;
import com.cadence.notificationservice.dto.response.EmailQueueItemResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import com.cadence.notificationservice.service.EmailQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Backs the Figma's "Email history" tab search box + status filter (§A3) -- same underlying data as EmailQueueController, exposed under the literal API path requested. */
@RestController
@RequestMapping("/api/v1/email-history")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Email History", description = "Searchable/filterable log of every email ever queued")
public class EmailHistoryController {

    private final EmailQueueService emailQueueService;

    @GetMapping
    @Operation(summary = "Search email history by recipient and/or status")
    public ResponseEntity<ApiResponse<PagedResponse<EmailQueueItemResponse>>> list(
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) EmailStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", emailQueueService.listEmails(recipient, status, pageable)));
    }
}
