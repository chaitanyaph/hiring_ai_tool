package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Read-only side backing the Assessments list/details drawer, the coding queue tab, and the candidate-facing history/intro/result screens. */
public interface AssessmentQueryService {

    PagedResponse<AssessmentListItemResponse> listAssessments(UUID companyId, AssessmentStatus status, UUID jobId, Pageable pageable);

    AssessmentResponse getAssessment(UUID companyId, UUID assessmentId);

    AssessmentDetailsResponse getAssessmentDetails(UUID companyId, UUID assessmentId);

    PagedResponse<CodingQueueItemResponse> getQueue(UUID companyId, UUID assessmentId, CandidateAssessmentStatus status, Pageable pageable);

    PagedResponse<CandidateAssessmentHistoryItemResponse> getCandidateHistory(UUID candidateId, Pageable pageable);

    CandidateAssessmentIntroResponse getCandidateIntro(UUID candidateAssessmentId);

    AssessmentResultResponse getCandidateResult(UUID candidateAssessmentId);
}
