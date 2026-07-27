package com.cadence.aiinterviewservice.service;

import com.cadence.aiinterviewservice.dto.response.PagedResponse;
import com.cadence.aiinterviewservice.dto.response.ShortlistItemResponse;
import com.cadence.aiinterviewservice.dto.response.ShortlistSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Read-only side backing the AI Shortlisting screen's Shortlisted/Rejected/Manual-review tabs, KPI row, and Ranking ("Top Candidates") table. */
public interface ShortlistQueryService {

    PagedResponse<ShortlistItemResponse> getShortlisted(UUID jobId, Pageable pageable);

    PagedResponse<ShortlistItemResponse> getRejected(UUID jobId, Pageable pageable);

    PagedResponse<ShortlistItemResponse> getManualReview(UUID jobId, Pageable pageable);

    ShortlistSummaryResponse getSummary(UUID jobId);

    List<ShortlistItemResponse> getRanking(UUID jobId);
}
