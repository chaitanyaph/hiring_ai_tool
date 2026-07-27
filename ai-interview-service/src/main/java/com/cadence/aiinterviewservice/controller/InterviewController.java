package com.cadence.aiinterviewservice.controller;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import com.cadence.aiinterviewservice.dto.response.*;
import com.cadence.aiinterviewservice.service.InterviewQueryService;
import com.cadence.aiinterviewservice.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Backs the AI Interviews screen: the interview queue, the analysis dashboard, the evaluation report drawer, and the recruiter's post-report decision actions. */
@RestController
@RequestMapping("/api/v1/ai-interviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "AI Interviews", description = "Interview queue, analysis dashboard, evaluation report, and post-report decisions")
public class InterviewController {

    private final InterviewQueryService interviewQueryService;
    private final InterviewSessionService interviewSessionService;

    @GetMapping("/queue/{jobId}")
    @Operation(summary = "List the AI interview queue for a job", description = "Filter by status: NOT_STARTED / IN_PROGRESS / COMPLETED / EXPIRED")
    public ResponseEntity<ApiResponse<PagedResponse<InterviewQueueItemResponse>>> getQueue(
            @PathVariable UUID jobId, @RequestParam(required = false) InterviewSessionStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getQueue(jobId, status, pageable)));
    }

    @GetMapping("/analysis-summary/{jobId}")
    @Operation(summary = "Get the Analysis dashboard KPI row for a job")
    public ResponseEntity<ApiResponse<InterviewAnalysisSummaryResponse>> getAnalysisSummary(@PathVariable UUID jobId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getAnalysisSummary(jobId)));
    }

    @GetMapping("/completed/{jobId}")
    @Operation(summary = "List completed interviews for a job (Analysis dashboard table)")
    public ResponseEntity<ApiResponse<PagedResponse<InterviewCompletedItemResponse>>> getCompleted(
            @PathVariable UUID jobId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getCompletedList(jobId, pageable)));
    }

    @GetMapping("/{applicationId}/report")
    @Operation(summary = "Get the full AI interview evaluation report (drawer)")
    public ResponseEntity<ApiResponse<InterviewEvaluationReportResponse>> getReport(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewQueryService.getReport(applicationId)));
    }

    @PostMapping("/{applicationId}/start")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Start the AI interview (sends the interview invitation to the candidate)")
    public ResponseEntity<ApiResponse<Void>> start(@PathVariable UUID applicationId,
                                                    @RequestParam UUID jobId, @RequestParam UUID candidateId) {
        interviewSessionService.inviteCandidate(applicationId, jobId, candidateId);
        return ResponseEntity.accepted().body(ApiResponse.ok("Interview invitation sent"));
    }

    @PostMapping("/{applicationId}/send-reminder")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Send a reminder for a not-started interview")
    public ResponseEntity<ApiResponse<Void>> sendReminder(@PathVariable UUID applicationId) {
        interviewSessionService.sendReminder(applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Reminder sent"));
    }

    @PostMapping("/{applicationId}/resend-invite")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Resend the invitation for an expired interview")
    public ResponseEntity<ApiResponse<Void>> resendInvite(@PathVariable UUID applicationId) {
        interviewSessionService.resendInvite(applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Invitation resent"));
    }

    @PostMapping("/{applicationId}/move-to-coding")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Move the candidate to the Coding Assessment stage")
    public ResponseEntity<ApiResponse<Void>> moveToCoding(@PathVariable UUID applicationId) {
        interviewSessionService.recordRecruiterDecision(applicationId, HiringRecommendation.PROCEED);
        return ResponseEntity.ok(ApiResponse.ok("Candidate moved to Coding Assessment"));
    }

    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Reject the candidate after reviewing the interview report")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable UUID applicationId) {
        interviewSessionService.recordRecruiterDecision(applicationId, HiringRecommendation.REJECT);
        return ResponseEntity.ok(ApiResponse.ok("Candidate rejected"));
    }

    @PostMapping("/{applicationId}/manual-review")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Flag the candidate's interview report for manual review")
    public ResponseEntity<ApiResponse<Void>> manualReview(@PathVariable UUID applicationId) {
        interviewSessionService.recordRecruiterDecision(applicationId, HiringRecommendation.HOLD);
        return ResponseEntity.ok(ApiResponse.ok("Flagged for manual review"));
    }
}
