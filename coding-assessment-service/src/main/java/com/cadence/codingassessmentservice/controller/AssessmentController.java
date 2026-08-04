package com.cadence.codingassessmentservice.controller;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.dto.request.AssignQuestionsRequest;
import com.cadence.codingassessmentservice.dto.request.CreateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.response.*;
import com.cadence.codingassessmentservice.security.CurrentUserProvider;
import com.cadence.codingassessmentservice.service.AssessmentQueryService;
import com.cadence.codingassessmentservice.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Backs the Coding Assessments hub: the 4-step creation wizard, the Assessments list/details drawer, the coding queue tab, and reminders. */
@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Assessments", description = "Assessment definition CRUD, publish/clone/archive/close, question assignment, queue, and reminders")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentQueryService assessmentQueryService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Create an assessment (wizard step 4 -- save as draft or publish now)")
    public ResponseEntity<ApiResponse<AssessmentResponse>> create(@Valid @RequestBody CreateAssessmentRequest request) {
        var user = currentUserProvider.getCurrentUser();
        var response = assessmentService.createAssessment(user.getCompanyId(), user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Assessment created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update a draft assessment")
    public ResponseEntity<ApiResponse<AssessmentResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateAssessmentRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Assessment updated", assessmentService.updateAssessment(companyId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete a draft assessment")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.deleteAssessment(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Assessment deleted"));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Publish a draft assessment", description = "Invites every eligible (AI-interview-recommended) candidate for the job")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.publishAssessment(companyId, id);
        return ResponseEntity.accepted().body(ApiResponse.ok("Assessment published"));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Clone an assessment into a new draft")
    public ResponseEntity<ApiResponse<AssessmentResponse>> clone(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Assessment cloned", assessmentService.cloneAssessment(companyId, id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Archive an assessment")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.archiveAssessment(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Assessment archived"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Close an assessment")
    public ResponseEntity<ApiResponse<Void>> close(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.closeAssessment(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Assessment closed"));
    }

    @PutMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Assign the question bank selection to an assessment")
    public ResponseEntity<ApiResponse<Void>> assignQuestions(@PathVariable UUID id, @Valid @RequestBody AssignQuestionsRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.assignQuestions(companyId, id, request.getQuestionIds());
        return ResponseEntity.ok(ApiResponse.ok("Questions assigned"));
    }

    @GetMapping
    @Operation(summary = "List assessments (Assessments tab table)", description = "Optionally filter by jobId -- used by the job creation wizard to check whether a job already has an assessment")
    public ResponseEntity<ApiResponse<PagedResponse<AssessmentListItemResponse>>> list(
            @RequestParam(required = false) AssessmentStatus status, @RequestParam(required = false) UUID jobId,
            @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.listAssessments(companyId, status, jobId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an assessment (details drawer header fields)")
    public ResponseEntity<ApiResponse<AssessmentResponse>> get(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getAssessment(companyId, id)));
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "Get the full assessment details drawer (rules + invited candidates)")
    public ResponseEntity<ApiResponse<AssessmentDetailsResponse>> getDetails(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getAssessmentDetails(companyId, id)));
    }

    @GetMapping("/{id}/queue")
    @Operation(summary = "Get the coding queue for an assessment", description = "Filter by status: NOT_STARTED / IN_PROGRESS / COMPLETED / EXPIRED")
    public ResponseEntity<ApiResponse<PagedResponse<CodingQueueItemResponse>>> getQueue(
            @PathVariable UUID id, @RequestParam(required = false) CandidateAssessmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", assessmentQueryService.getQueue(companyId, id, status, pageable)));
    }

    @PostMapping("/{id}/send-reminders")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Send reminders to every not-started candidate")
    public ResponseEntity<ApiResponse<Void>> sendReminders(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.sendReminders(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Reminders sent"));
    }

    @PostMapping("/{id}/candidates/{applicationId}/remind")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Remind one candidate (or resend an expired invite)")
    public ResponseEntity<ApiResponse<Void>> remindCandidate(@PathVariable UUID id, @PathVariable UUID applicationId) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        assessmentService.remindCandidate(companyId, id, applicationId);
        return ResponseEntity.ok(ApiResponse.ok("Reminder sent"));
    }
}
