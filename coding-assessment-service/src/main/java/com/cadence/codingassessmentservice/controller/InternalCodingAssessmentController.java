package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.dto.response.ApiResponse;
import com.cadence.codingassessmentservice.dto.response.SubmissionHistoryItemResponse;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.SubmissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * True machine-to-machine endpoint -- trusted network only, permitAll
 * (see SecurityConstants.PUBLIC_ENDPOINTS), same trust model as every
 * other service's /internal API. Built for a future Analytics service
 * to read a candidate's coding assessment outcome without a JWT.
 */
@RestController
@RequestMapping("/api/v1/internal/coding-assessments")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Coding assessment outcome lookup -- trusted network only")
public class InternalCodingAssessmentController {

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final SubmissionQueryService submissionQueryService;

    @GetMapping("/{applicationId}")
    @Operation(summary = "Get the submission history for an application's coding assessment attempt")
    public ResponseEntity<ApiResponse<List<SubmissionHistoryItemResponse>>> getByApplication(@PathVariable UUID applicationId) {
        List<CandidateAssessment> attempts = candidateAssessmentRepository.findAllByApplicationId(applicationId);
        CandidateAssessment ca = attempts.stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No coding assessment found for application " + applicationId));
        return ResponseEntity.ok(ApiResponse.ok("OK", submissionQueryService.getSubmissionHistory(ca.getId())));
    }
}
