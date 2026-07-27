package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Backs sec-analytics's "Recruiter performance" table. hiresCount and avgTimeToHireDays are
 * always 0/null on every row -- RecruiterAssignedEvent has no completion linkage back to it
 * (see MetricIngestionServiceImpl#onRecruiterAssigned), flagged rather than fabricated.
 */
public interface RecruiterPerformanceService {

    List<RecruiterPerformanceResponse> getRecruiterPerformance(UUID companyId);
}
