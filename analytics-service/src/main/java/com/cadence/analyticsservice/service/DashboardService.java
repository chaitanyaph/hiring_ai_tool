package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.dto.response.DashboardResponse;

import java.util.UUID;

/**
 * Backs all 5 dashboard endpoints (§13 screen-to-API mapping). Recruiter/HR/Hiring-Manager
 * dashboards reuse the same COMPANY-scoped read as the company dashboard -- metric_snapshot
 * has no RECRUITER-scoped funnel/KPI rows (only recruiter_performance_snapshot is per-recruiter),
 * so true per-recruiter filtering of the whole dashboard isn't backed by any ingested data.
 * Flagged here and in README rather than fabricating a narrower scope.
 */
public interface DashboardService {

    DashboardResponse getExecutiveDashboard();

    DashboardResponse getCompanyDashboard(UUID companyId);

    DashboardResponse getRecruiterDashboard(UUID companyId);

    DashboardResponse getHrDashboard(UUID companyId);

    DashboardResponse getHiringManagerDashboard(UUID companyId);
}
