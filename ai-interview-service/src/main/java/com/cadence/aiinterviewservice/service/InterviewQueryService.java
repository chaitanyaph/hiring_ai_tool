package com.cadence.aiinterviewservice.service;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import com.cadence.aiinterviewservice.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Read-only side backing the AI Interviews queue/dashboard, the evaluation report drawer, and the candidate-facing interview screens. */
public interface InterviewQueryService {

    PagedResponse<InterviewQueueItemResponse> getQueue(UUID jobId, InterviewSessionStatus status, Pageable pageable);

    InterviewAnalysisSummaryResponse getAnalysisSummary(UUID jobId);

    PagedResponse<InterviewCompletedItemResponse> getCompletedList(UUID jobId, Pageable pageable);

    InterviewEvaluationReportResponse getReport(UUID applicationId);

    InterviewDetailsResponse getCandidateDetails(UUID applicationId);

    InterviewResultResponse getCandidateResult(UUID applicationId);
}
