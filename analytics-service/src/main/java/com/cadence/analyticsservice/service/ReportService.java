package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.dto.response.ReportResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Backs /reports/daily, /monthly, /yearly (§13). companyId is optional -- null yields a
 * platform-wide (GLOBAL scope) report for admin use, matching the DomainAnalyticsService
 * convention. See ReportResponse javadoc for the as-of-snapshot vs. period-sliced distinction.
 */
public interface ReportService {

    ReportResponse getDailyReport(UUID companyId, LocalDate date);

    ReportResponse getMonthlyReport(UUID companyId, LocalDate month);

    ReportResponse getYearlyReport(UUID companyId, int year);
}
