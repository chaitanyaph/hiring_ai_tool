package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.response.CodingResultsSummaryResponse;
import com.cadence.codingassessmentservice.dto.response.LeaderboardItemResponse;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Read-only side backing Tab 3's KPI row, "Candidate ranking" card, and "Completed assessments" list. */
public interface LeaderboardService {

    CodingResultsSummaryResponse getResultsSummary(UUID assessmentId);

    List<LeaderboardItemResponse> getLeaderboard(UUID assessmentId);

    PagedResponse<LeaderboardItemResponse> getCompletedList(UUID assessmentId, Pageable pageable);
}
