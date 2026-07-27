package com.cadence.interviewmanagementservice.controller;

import com.cadence.interviewmanagementservice.dto.request.RequestRescheduleRequest;
import com.cadence.interviewmanagementservice.dto.response.ApiResponse;
import com.cadence.interviewmanagementservice.dto.response.CandidateInterviewResponse;
import com.cadence.interviewmanagementservice.dto.response.CandidateTimelineResponse;
import com.cadence.interviewmanagementservice.security.CurrentUserProvider;
import com.cadence.interviewmanagementservice.service.CandidateTimelineService;
import com.cadence.interviewmanagementservice.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs #csec-interviews ("My interviews") and #drawer-cand-interview-detail. */
@RestController
@RequestMapping("/api/v1/candidate")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate Interviews", description = "My interviews list/detail, request reschedule, timeline")
public class CandidateInterviewController {

    private final InterviewService interviewService;
    private final CandidateTimelineService candidateTimelineService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/interviews")
    @Operation(summary = "List my interviews (Upcoming/Past filter tabs)")
    public ResponseEntity<ApiResponse<List<CandidateInterviewResponse>>> list(@RequestParam(defaultValue = "false") boolean upcomingOnly) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewService.getCandidateInterviews(candidateId, upcomingOnly)));
    }

    @GetMapping("/interviews/{id}")
    @Operation(summary = "Get my interview detail")
    public ResponseEntity<ApiResponse<CandidateInterviewResponse>> get(@PathVariable UUID id) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewService.getCandidateInterviewDetail(candidateId, id)));
    }

    @PostMapping("/interviews/{id}/request-reschedule")
    @Operation(summary = "Request a reschedule", description = "Logged for the recruiter to see -- mirrors the Figma's mockToast-only behavior, not auto-actioned")
    public ResponseEntity<ApiResponse<Void>> requestReschedule(@PathVariable UUID id, @RequestBody(required = false) RequestRescheduleRequest request) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        RequestRescheduleRequest body = request != null ? request : RequestRescheduleRequest.builder().build();
        interviewService.requestReschedule(candidateId, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Reschedule request sent"));
    }

    @GetMapping("/interviews/timeline/{applicationId}")
    @Operation(summary = "Get my hiring pipeline timeline for an application")
    public ResponseEntity<ApiResponse<List<CandidateTimelineResponse>>> timeline(@PathVariable UUID applicationId) {
        var candidateId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", candidateTimelineService.getMyTimeline(candidateId, applicationId)));
    }
}
