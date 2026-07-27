package com.cadence.resumeparserservice.controller;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.dto.response.ApiResponse;
import com.cadence.resumeparserservice.dto.response.PagedResponse;
import com.cadence.resumeparserservice.dto.response.ParsingQueueItemResponse;
import com.cadence.resumeparserservice.dto.response.ParsingQueueSummaryResponse;
import com.cadence.resumeparserservice.service.ParserQueueService;
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

/** Backs #sec-parsing: the filterable/searchable queue table and its KPI row. */
@RestController
@RequestMapping("/api/v1/parser/queue")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Parsing Queue", description = "Recruiter-facing view of every resume currently queued, processing, parsed or failed")
public class ParserQueueController {

    private final ParserQueueService parserQueueService;

    @GetMapping
    @Operation(summary = "List the parsing queue", description = "Filter by status and search by candidate name/email once parsed")
    public ResponseEntity<ApiResponse<PagedResponse<ParsingQueueItemResponse>>> getQueue(
            @RequestParam(required = false) ParsingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = parserQueueService.getQueue(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("OK", PagedResponse.from(page)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get queue KPI counts (Queued / Processing / Parsed today / Failed)")
    public ResponseEntity<ApiResponse<ParsingQueueSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok("OK", parserQueueService.getSummary()));
    }
}
