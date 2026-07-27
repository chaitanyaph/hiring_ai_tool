package com.cadence.resumeparserservice.controller;

import com.cadence.resumeparserservice.dto.response.ApiResponse;
import com.cadence.resumeparserservice.dto.response.ParsedResumeResponse;
import com.cadence.resumeparserservice.service.ParsedResumeQueryService;
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
 * True machine-to-machine endpoint -- trusted network only, permitAll
 * (see SecurityConstants.PUBLIC_ENDPOINTS), same trust model as every
 * other service's /internal API. Built for the not-yet-built Matching
 * and Shortlisting services to read parsed resume data without a JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/parser/resumes")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Parsed resume aggregate lookup -- trusted network only")
public class InternalParsedResumeController {

    private final ParsedResumeQueryService parsedResumeQueryService;

    @GetMapping("/{resumeId}")
    @Operation(summary = "Get the full parsed resume aggregate")
    public ResponseEntity<ApiResponse<ParsedResumeResponse>> getAggregate(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", parsedResumeQueryService.getAggregate(resumeId)));
    }
}
