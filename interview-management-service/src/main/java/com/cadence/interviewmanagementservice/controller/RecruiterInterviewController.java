package com.cadence.interviewmanagementservice.controller;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.PlatformRole;
import com.cadence.interviewmanagementservice.dto.request.*;
import com.cadence.interviewmanagementservice.dto.response.*;
import com.cadence.interviewmanagementservice.security.CurrentUserProvider;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import com.cadence.interviewmanagementservice.service.InterviewDecisionService;
import com.cadence.interviewmanagementservice.service.InterviewFeedbackService;
import com.cadence.interviewmanagementservice.service.InterviewService;
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

/** Backs #sec-interviews (upcoming/completed tabs), modal-interview, drawer-interview-detail, modal-reschedule-interview, modal-cancel-interview, modal-submit-feedback, drawer-interview-feedback-view. */
@RestController
@RequestMapping("/api/v1/recruiter/interviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Recruiter Interviews", description = "Schedule/reschedule/cancel technical, manager, HR interviews; feedback; decisions; timeline")
public class RecruiterInterviewController {

    private final InterviewService interviewService;
    private final InterviewFeedbackService interviewFeedbackService;
    private final InterviewDecisionService interviewDecisionService;
    private final CandidateTimelineService candidateTimelineService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Schedule an interview (modal-interview)")
    public ResponseEntity<ApiResponse<InterviewDetailResponse>> schedule(@Valid @RequestBody ScheduleInterviewRequest request) {
        var user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Interview scheduled -- invite sent",
                interviewService.scheduleInterview(user.getCompanyId(), user.getUserId(), request)));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Reschedule an interview (modal-reschedule-interview)")
    public ResponseEntity<ApiResponse<InterviewDetailResponse>> reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleInterviewRequest request) {
        var user = currentUserProvider.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Interview rescheduled",
                interviewService.rescheduleInterview(user.getCompanyId(), id, user.getUserId(), request)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Cancel an interview (modal-cancel-interview)")
    public ResponseEntity<ApiResponse<InterviewDetailResponse>> cancel(@PathVariable UUID id, @RequestBody(required = false) CancelInterviewRequest request) {
        var user = currentUserProvider.getCurrentUser();
        CancelInterviewRequest body = request != null ? request : CancelInterviewRequest.builder().build();
        return ResponseEntity.ok(ApiResponse.ok("Interview cancelled",
                interviewService.cancelInterview(user.getCompanyId(), id, user.getUserId(), body)));
    }

    @GetMapping
    @Operation(summary = "List interviews (upcoming/completed tabs)")
    public ResponseEntity<ApiResponse<PagedResponse<InterviewListItemResponse>>> list(
            @RequestParam(required = false) InterviewStatus status, @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewService.listInterviews(companyId, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get interview detail (drawer-interview-detail)")
    public ResponseEntity<ApiResponse<InterviewDetailResponse>> get(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewService.getInterview(companyId, id)));
    }

    @GetMapping("/{id}/activity")
    @Operation(summary = "Get the interview's activity/history log")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getActivity(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewService.getActivityLog(companyId, id)));
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "Submit interview feedback (modal-submit-feedback)")
    public ResponseEntity<ApiResponse<InterviewFeedbackResponse>> submitFeedback(@PathVariable UUID id, @Valid @RequestBody SubmitFeedbackRequest request) {
        var user = currentUserProvider.getCurrentUser();
        boolean isRecruitingRole = user.getRole() != null && PlatformRole.RECRUITING_ROLES.contains(user.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Feedback submitted",
                interviewFeedbackService.submitFeedback(user.getCompanyId(), id, user.getUserId(), isRecruitingRole, request)));
    }

    @GetMapping("/{id}/feedback")
    @Operation(summary = "View interview feedback (drawer-interview-feedback-view)")
    public ResponseEntity<ApiResponse<List<InterviewFeedbackResponse>>> getFeedback(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewFeedbackService.getFeedback(companyId, id)));
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Record a recruiter decision (Move to HR / Next round / Select / Reject / Hold / Request another interview)")
    public ResponseEntity<ApiResponse<Void>> decide(@PathVariable UUID id, @Valid @RequestBody RecruiterDecisionRequest request) {
        var user = currentUserProvider.getCurrentUser();
        interviewDecisionService.recordDecision(user.getCompanyId(), id, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Decision recorded"));
    }

    @GetMapping("/timeline/{applicationId}")
    @Operation(summary = "Get a candidate's hiring pipeline timeline for an application")
    public ResponseEntity<ApiResponse<List<CandidateTimelineResponse>>> timeline(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateTimelineService.getTimelineForApplication(applicationId)));
    }
}
