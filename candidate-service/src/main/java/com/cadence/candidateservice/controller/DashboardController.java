package com.cadence.candidateservice.controller;

import com.cadence.candidateservice.dto.response.ApiResponse;
import com.cadence.candidateservice.dto.response.DashboardResponse;
import com.cadence.candidateservice.security.CurrentUserProvider;
import com.cadence.candidateservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Dashboard", description = "Aggregated candidate home-screen data")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Get my dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("OK", dashboardService.getDashboard(currentUserProvider.getCurrentUser())));
    }
}
