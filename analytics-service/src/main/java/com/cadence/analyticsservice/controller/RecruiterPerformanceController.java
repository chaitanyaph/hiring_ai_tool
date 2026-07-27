package com.cadence.analyticsservice.controller;

import com.cadence.analyticsservice.dto.response.ApiResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.security.CurrentUserProvider;
import com.cadence.analyticsservice.service.RecruiterPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruiter-performance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Recruiter Performance", description = "sec-analytics's recruiter performance table")
public class RecruiterPerformanceController {

    private final RecruiterPerformanceService recruiterPerformanceService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Recruiter performance table for the caller's company")
    public ResponseEntity<ApiResponse<List<RecruiterPerformanceResponse>>> list() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", recruiterPerformanceService.getRecruiterPerformance(companyId)));
    }
}
