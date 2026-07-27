package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.dto.response.*;
import com.cadence.codingassessmentservice.security.CurrentUserProvider;
import com.cadence.codingassessmentservice.service.AnalyticsService;
import com.cadence.codingassessmentservice.service.AssessmentService;
import com.cadence.codingassessmentservice.service.LeaderboardService;
import com.cadence.codingassessmentservice.service.SubmissionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs the Results tab's KPI row + candidate ranking + completed list, the Analytics tab, and the submission drawer. */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Coding Results", description = "Results KPIs, leaderboard, analytics, and the recruiter submission drawer")
public class CodingResultsController {

    private final LeaderboardService leaderboardService;
    private final AnalyticsService analyticsService;
    private final SubmissionQueryService submissionQueryService;
    private final AssessmentService assessmentService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/api/v1/assessments/{id}/results/summary")
    @Operation(summary = "Get the Results tab KPI row")
    public ResponseEntity<ApiResponse<CodingResultsSummaryResponse>> getResultsSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", leaderboardService.getResultsSummary(id)));
    }

    @GetMapping("/api/v1/leaderboard/{assessmentId}")
    @Operation(summary = "Get the candidate ranking for an assessment")
    public ResponseEntity<ApiResponse<List<LeaderboardItemResponse>>> getLeaderboard(@PathVariable UUID assessmentId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", leaderboardService.getLeaderboard(assessmentId)));
    }

    @GetMapping("/api/v1/submissions/{assessmentId}")
    @Operation(summary = "List completed assessments for an assessment (Results tab)")
    public ResponseEntity<ApiResponse<PagedResponse<LeaderboardItemResponse>>> getCompleted(
            @PathVariable UUID assessmentId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", leaderboardService.getCompletedList(assessmentId, pageable)));
    }

    @GetMapping("/api/v1/submissions/{assessmentId}/{applicationId}")
    @Operation(summary = "Get the full submission drawer for a candidate")
    public ResponseEntity<ApiResponse<SubmissionDrawerResponse>> getSubmissionDrawer(
            @PathVariable UUID assessmentId, @PathVariable UUID applicationId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", submissionQueryService.getSubmissionDrawer(companyId, assessmentId, applicationId)));
    }

    @PostMapping("/api/v1/submissions/{assessmentId}/{applicationId}/move-to-next-stage")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Move a completed candidate to the next pipeline stage")
    public ResponseEntity<ApiResponse<Void>> moveToNextStage(@PathVariable UUID assessmentId, @PathVariable UUID applicationId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.moveToNextStage(companyId, assessmentId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate moved to next stage"));
    }

    @GetMapping("/api/v1/analytics/{assessmentId}")
    @Operation(summary = "Get the Analytics tab (completion rate, success rate, language/difficulty breakdowns)")
    public ResponseEntity<ApiResponse<CodingAnalyticsResponse>> getAnalytics(@PathVariable UUID assessmentId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", analyticsService.getAnalytics(assessmentId)));
    }

    @GetMapping("/api/v1/submissions/detail/{submissionId}/ai-review")
    @Operation(summary = "Get the full structured AI code review for a submission (\"Code review\" / \"AI analysis\" actions)")
    public ResponseEntity<ApiResponse<AiCodeReviewResponse>> getAiCodeReview(@PathVariable UUID submissionId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", submissionQueryService.getAiCodeReview(submissionId)));
    }
}
