package com.cadence.analyticsservice.service;

import com.cadence.analyticsservice.dto.response.AssessmentAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.CandidateAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.InterviewAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.JobAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.OfferAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.ResumeAnalyticsResponse;

import java.util.UUID;

/**
 * Backs the 6 domain-analytics endpoints (§13). Job/candidate volumes carry companyId on
 * their source events and are company-scoped when companyId is supplied (GLOBAL/platform-wide
 * otherwise). Resume/interview/assessment/offer score and count metrics are GLOBAL only -- those
 * source events never carry companyId (see MetricIngestionServiceImpl javadoc), so per-company
 * breakdowns for those four domains are not exposed rather than being silently mislabeled.
 */
public interface DomainAnalyticsService {

    JobAnalyticsResponse getJobAnalytics(UUID companyId);

    CandidateAnalyticsResponse getCandidateAnalytics(UUID companyId);

    ResumeAnalyticsResponse getResumeAnalytics();

    InterviewAnalyticsResponse getInterviewAnalytics();

    AssessmentAnalyticsResponse getAssessmentAnalytics();

    OfferAnalyticsResponse getOfferAnalytics();
}
