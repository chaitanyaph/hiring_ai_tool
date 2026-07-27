package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.response.CodingAnalyticsResponse;

import java.util.UUID;

/** Read-only side backing Tab 4's KPI row and the difficulty/language breakdown cards. */
public interface AnalyticsService {

    CodingAnalyticsResponse getAnalytics(UUID assessmentId);
}
