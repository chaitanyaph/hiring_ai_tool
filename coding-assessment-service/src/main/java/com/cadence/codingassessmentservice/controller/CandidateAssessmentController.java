package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.dto.request.*;
import com.cadence.codingassessmentservice.dto.response.*;
import com.cadence.codingassessmentservice.security.CurrentUserProvider;
import com.cadence.codingassessmentservice.service.AssessmentQueryService;
import com.cadence.codingassessmentservice.service.CandidateAssessmentService;
import com.cadence.codingassessmentservice.service.CodeExecutionService;
import com.cadence.codingassessmentservice.service.SubmissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs the candidate-facing coding assessment history, the intro/rules screens, and the full live IDE turn-based flow. */
@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Coding Assessment", description = "History, intro/rules, live IDE run/submit, and result")
public class CandidateAssessmentController {

    private final AssessmentQueryService assessmentQueryService;
    private final CandidateAssessmentService candidateAssessmentService;
    private final CodeExecutionService codeExecutionService;
    private final SubmissionQueryService submissionQueryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/assessments")
    @Operation(summary = "List my coding assessment history")
    public ResponseEntity<ApiResponse<PagedResponse<CandidateAssessmentHistoryItemResponse>>> getHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getCandidateHistory(candidateId, pageable)));
    }

    @GetMapping("/assessments/{id}")
    @Operation(summary = "Get the intro/setup screen for an assessment attempt")
    public ResponseEntity<ApiResponse<CandidateAssessmentIntroResponse>> getIntro(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getCandidateIntro(id)));
    }

    @PostMapping("/assessments/{id}/accept-rules")
    @Operation(summary = "Accept the assessment rules")
    public ResponseEntity<ApiResponse<Void>> acceptRules(@PathVariable UUID id) {
        candidateAssessmentService.acceptRules(id);
        return ResponseEntity.ok(ApiResponse.ok("Rules accepted"));
    }

    @PostMapping("/assessments/{id}/start")
    @Operation(summary = "Start the assessment and receive the first question")
    public ResponseEntity<ApiResponse<IdeQuestionResponse>> start(@PathVariable UUID id, @RequestBody(required = false) StartAssessmentRequest request) {
        var preferredLanguage = request != null ? request.getPreferredLanguage() : null;
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateAssessmentService.startAssessment(id, preferredLanguage)));
    }

    @GetMapping("/assessments/{id}/questions/{questionIndex}")
    @Operation(summary = "Navigate to a question (Question Navigator pill click)")
    public ResponseEntity<ApiResponse<IdeQuestionResponse>> goToQuestion(@PathVariable UUID id, @PathVariable int questionIndex) {
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateAssessmentService.goToQuestion(id, questionIndex)));
    }

    @PostMapping("/run")
    @Operation(summary = "Compile & Run against sample/custom input (unscored)")
    public ResponseEntity<ApiResponse<RunCodeResponse>> run(@Valid @RequestBody RunCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("OK", codeExecutionService.runCode(request)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit code for a question (scored against visible + hidden test cases)")
    public ResponseEntity<ApiResponse<SubmitCodeResponse>> submit(@Valid @RequestBody SubmitCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("OK", codeExecutionService.submitCode(request)));
    }

    @PostMapping("/assessments/{id}/mark-for-review")
    @Operation(summary = "Toggle \"Mark for review\" on a question")
    public ResponseEntity<ApiResponse<Void>> markForReview(@PathVariable UUID id, @Valid @RequestBody MarkForReviewRequest request) {
        candidateAssessmentService.markForReview(id, request.getQuestionId(), request.isMarked());
        return ResponseEntity.ok(ApiResponse.ok("Updated"));
    }

    @PostMapping("/assessments/{id}/finish")
    @Operation(summary = "Submit the whole assessment (confirm modal, or timer auto-submit)")
    public ResponseEntity<ApiResponse<Void>> finish(@PathVariable UUID id, @RequestBody(required = false) FinishAssessmentRequest request) {
        boolean autoSubmitted = request != null && request.isAutoSubmitted();
        candidateAssessmentService.finishAssessment(id, autoSubmitted);
        return ResponseEntity.accepted().body(ApiResponse.ok("Assessment submitted"));
    }

    @GetMapping("/submissions/{id}")
    @Operation(summary = "Get my submission history for this assessment attempt")
    public ResponseEntity<ApiResponse<List<SubmissionHistoryItemResponse>>> getSubmissionHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", submissionQueryService.getSubmissionHistory(id)));
    }

    @GetMapping("/result/{id}")
    @Operation(summary = "Get my result for this assessment attempt")
    public ResponseEntity<ApiResponse<AssessmentResultResponse>> getResult(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getCandidateResult(id)));
    }

    @PostMapping("/assessments/{id}/anti-cheat-events")
    @Operation(summary = "Log an anti-cheat signal (tab switch, fullscreen exit, copy, paste, ...)")
    public ResponseEntity<ApiResponse<Void>> recordAntiCheatEvent(@PathVariable UUID id, @Valid @RequestBody AntiCheatEventRequest request) {
        candidateAssessmentService.recordAntiCheatEvent(id, request.getEventType(), request.getMetadata());
        return ResponseEntity.ok(ApiResponse.ok("Logged"));
    }
}
