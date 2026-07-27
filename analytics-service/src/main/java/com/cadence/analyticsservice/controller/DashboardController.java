package com.cadence.analyticsservice.controller;

import com.cadence.analyticsservice.dto.response.ApiResponse;
import com.cadence.analyticsservice.dto.response.DashboardResponse;
import com.cadence.analyticsservice.security.CurrentUserProvider;
import com.cadence.analyticsservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Backs the 5 role-scoped dashboard screens (§13). Recruiter/HR/Hiring-Manager reuse the company-scoped read -- see DashboardService javadoc. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Executive/company/recruiter/hr/hiring-manager analytics dashboards")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/executive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Platform-wide executive dashboard (GLOBAL scope)")
    public ResponseEntity<ApiResponse<DashboardResponse>> executive() {
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getExecutiveDashboard()));
    }

    @GetMapping("/company")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
    @Operation(summary = "Company-scoped dashboard for the caller's own company")
    public ResponseEntity<ApiResponse<DashboardResponse>> company() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getCompanyDashboard(companyId)));
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Recruiter-facing dashboard (company-scoped)")
    public ResponseEntity<ApiResponse<DashboardResponse>> recruiter() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getRecruiterDashboard(companyId)));
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','ADMIN')")
    @Operation(summary = "HR-facing dashboard (company-scoped)")
    public ResponseEntity<ApiResponse<DashboardResponse>> hr() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getHrDashboard(companyId)));
    }

    @GetMapping("/hiring-manager")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HIRING_MANAGER','ADMIN')")
    @Operation(summary = "Hiring-manager-facing dashboard (company-scoped)")
    public ResponseEntity<ApiResponse<DashboardResponse>> hiringManager() {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getHiringManagerDashboard(companyId)));
    }
}
