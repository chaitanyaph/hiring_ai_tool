package com.cadence.aiinterviewservice.controller;

import com.cadence.aiinterviewservice.dto.request.AnswerRequest;
import com.cadence.aiinterviewservice.dto.request.StartInterviewRequest;
import com.cadence.aiinterviewservice.dto.response.ApiResponse;
import com.cadence.aiinterviewservice.dto.response.InterviewDetailsResponse;
import com.cadence.aiinterviewservice.dto.response.InterviewQuestionResponse;
import com.cadence.aiinterviewservice.dto.response.InterviewResultResponse;
import com.cadence.aiinterviewservice.security.CurrentUserProvider;
import com.cadence.aiinterviewservice.service.InterviewQueryService;
import com.cadence.aiinterviewservice.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Backs the candidate-facing AI interview flow: intro/setup, the live
 * turn-based question loop, and the completion/result screens. Every method
 * resolves the CURRENT candidate from the JWT and passes it into the service
 * layer, which verifies it against the session's own candidateId -- without
 * this, any authenticated CANDIDATE could read/start/answer/finish/view-
 * result for any other candidate's applicationId just by guessing/enumerating
 * IDs (IDOR), since applicationId alone was previously trusted as sufficient.
 */
@RestController
@RequestMapping("/api/v1/candidate/interview")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Interview", description = "The candidate's own AI interview: details, start, answer, finish, result")
public class CandidateInterviewController {

    private final InterviewQueryService interviewQueryService;
    private final InterviewSessionService interviewSessionService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/details")
    @Operation(summary = "Get the interview intro/setup details (mode options, question count, estimated duration)")
    public ResponseEntity<ApiResponse<InterviewDetailsResponse>> getDetails(@RequestParam UUID applicationId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getCandidateDetails(applicationId, candidateId)));
    }

    @PostMapping("/start")
    @Operation(summary = "Start the interview and receive the first question")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> start(@Valid @RequestBody StartInterviewRequest request) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewSessionService.startInterview(request.getApplicationId(), candidateId, request.getMode())));
    }

    @PostMapping("/answer")
    @Operation(summary = "Submit an answer and receive the next question (or completion)")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> answer(@Valid @RequestBody AnswerRequest request) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewSessionService.submitAnswer(request.getApplicationId(), candidateId, request)));
    }

    @PostMapping("/finish")
    @Operation(summary = "End the interview early")
    public ResponseEntity<ApiResponse<Void>> finish(@RequestParam UUID applicationId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        interviewSessionService.finishInterview(applicationId, candidateId);
        return ResponseEntity.ok(ApiResponse.ok("Interview finished"));
    }

    @GetMapping("/result")
    @Operation(summary = "Get the interview result / transcript")
    public ResponseEntity<ApiResponse<InterviewResultResponse>> result(@RequestParam UUID applicationId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getCandidateResult(applicationId, candidateId)));
    }
}
