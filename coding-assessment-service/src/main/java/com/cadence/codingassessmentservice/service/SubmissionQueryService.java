package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.response.AiCodeReviewResponse;
import com.cadence.codingassessmentservice.dto.response.SubmissionDrawerResponse;
import com.cadence.codingassessmentservice.dto.response.SubmissionHistoryItemResponse;

import java.util.List;
import java.util.UUID;

/** Read-only side backing the recruiter's submission drawer and the candidate's submission-history modal / "Code review" and "AI analysis" actions. */
public interface SubmissionQueryService {

    SubmissionDrawerResponse getSubmissionDrawer(UUID companyId, UUID assessmentId, UUID applicationId);

    List<SubmissionHistoryItemResponse> getSubmissionHistory(UUID candidateAssessmentId);

    AiCodeReviewResponse getAiCodeReview(UUID submissionId);
}
