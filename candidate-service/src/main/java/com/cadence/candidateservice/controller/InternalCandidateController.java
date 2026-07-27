package com.cadence.candidateservice.controller;

import com.cadence.candidateservice.dto.response.ApiResponse;
import com.cadence.candidateservice.dto.response.CandidateSummaryResponse;
import com.cadence.candidateservice.service.CandidateProfileService;
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
 * Trusted-network, service-to-service endpoint -- permitAll (see
 * SecurityConstants.PUBLIC_ENDPOINTS), same trust model as Company
 * Service which has no auth on any endpoint. Only exposes non-sensitive
 * completion signals, never PII, so other services (e.g. Application
 * Service validating apply() eligibility) don't need the candidate's
 * own bearer token to call it.
 */
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Service-to-service candidate lookups")
public class InternalCandidateController {

    private final CandidateProfileService candidateProfileService;

    @GetMapping("/{candidateId}/summary")
    @Operation(summary = "Get a candidate's profile-completion summary (internal, no PII)")
    public ResponseEntity<ApiResponse<CandidateSummaryResponse>> getSummary(@PathVariable UUID candidateId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateProfileService.getSummary(candidateId)));
    }
}
