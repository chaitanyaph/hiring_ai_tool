package com.cadence.analyticsservice.controller;

import com.cadence.analyticsservice.dto.response.ApiResponse;
import com.cadence.analyticsservice.dto.response.FunnelResponse;
import com.cadence.analyticsservice.security.CurrentUserProvider;
import com.cadence.analyticsservice.service.FunnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backs sec-analytics's interactive funnel widget -- see FunnelService javadoc for the no-job-filter limitation. */
@RestController
@RequestMapping("/api/v1/funnel")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Funnel", description = "Company-scoped hiring funnel")
public class FunnelController {

    private final FunnelService funnelService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Get the caller's company-scoped hiring funnel")
    public ResponseEntity<ApiResponse<FunnelResponse>> getFunnel() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", funnelService.getCompanyFunnel(companyId)));
    }
}
