package com.cadence.jobservice.controller;

import com.cadence.jobservice.constant.EmploymentType;
import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.dto.request.*;
import com.cadence.jobservice.dto.response.ApiResponse;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobSummaryResponse;
import com.cadence.jobservice.dto.response.PagedResponse;
import com.cadence.jobservice.security.CurrentUserProvider;
import com.cadence.jobservice.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Only the 6 roles listed in the platform's Job Management access rules
 * can reach this controller at all -- Interviewers and Candidates are
 * implicitly denied since they're not in that allow-list. Hiring
 * Manager's "view only unless granted edit permission" nuance is
 * enforced inside JobService (requireWriteAccess), since it depends on
 * a per-user permission claim, not just the role name.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
@Tag(name = "Jobs", description = "Job posting lifecycle -- wizard steps, publish/pause/close/archive/restore, search")
public class JobController {

    private final JobService jobService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Create a job draft (wizard Step 1)")
    public ResponseEntity<ApiResponse<JobDetailResponse>> createDraft(@Valid @RequestBody JobBasicInfoRequest request) {
        JobDetailResponse response = jobService.createDraft(request, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Job draft created", response));
    }

    @PutMapping("/{id}/basic-info")
    @Operation(summary = "Update basic info (wizard Step 1 / autosave)")
    public ResponseEntity<ApiResponse<JobDetailResponse>> updateBasicInfo(@PathVariable UUID id, @Valid @RequestBody JobBasicInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Basic info updated", jobService.updateBasicInfo(id, request, currentUserProvider.getCurrentUser())));
    }

    @PutMapping("/{id}/requirements")
    @Operation(summary = "Update requirements (wizard Step 2)")
    public ResponseEntity<ApiResponse<JobDetailResponse>> updateRequirements(@PathVariable UUID id, @Valid @RequestBody JobRequirementsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Requirements updated", jobService.updateRequirements(id, request, currentUserProvider.getCurrentUser())));
    }

    @PutMapping("/{id}/pipeline-stages")
    @Operation(summary = "Update hiring pipeline stages (wizard Step 3) -- full replace, supports add/delete/reorder/rename/enable-disable")
    public ResponseEntity<ApiResponse<JobDetailResponse>> updatePipelineStages(@PathVariable UUID id, @Valid @RequestBody UpdatePipelineStagesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Pipeline stages updated", jobService.updatePipelineStages(id, request, currentUserProvider.getCurrentUser())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full job detail (wizard Step 4 review, or general view/edit load)")
    public ResponseEntity<ApiResponse<JobDetailResponse>> getJobDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.getJobDetail(id, currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a draft job", description = "Validates department, openings, recruiter assignment, deadline and at least one enabled stage before allowing the transition")
    public ResponseEntity<ApiResponse<JobDetailResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Job published", jobService.publishJob(id, currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a published job")
    public ResponseEntity<ApiResponse<JobDetailResponse>> pause(@PathVariable UUID id, @RequestBody(required = false) StatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job paused", jobService.pauseJob(id, request != null ? request : new StatusChangeRequest(), currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a paused job back to Published")
    public ResponseEntity<ApiResponse<JobDetailResponse>> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Job resumed", jobService.resumeJob(id, currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a job (position filled/cancelled)")
    public ResponseEntity<ApiResponse<JobDetailResponse>> close(@PathVariable UUID id, @RequestBody(required = false) StatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job closed", jobService.closeJob(id, request != null ? request : new StatusChangeRequest(), currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a job")
    public ResponseEntity<ApiResponse<JobDetailResponse>> archive(@PathVariable UUID id, @RequestBody(required = false) StatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job archived", jobService.archiveJob(id, request != null ? request : new StatusChangeRequest(), currentUserProvider.getCurrentUser())));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore an archived job back to Draft")
    public ResponseEntity<ApiResponse<JobDetailResponse>> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Job restored", jobService.restoreJob(id, currentUserProvider.getCurrentUser())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft job", description = "Only DRAFT jobs can be deleted -- archive a published/closed job instead")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        jobService.deleteJob(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.ok(ApiResponse.ok("Job deleted"));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate/clone a job into a new draft")
    public ResponseEntity<ApiResponse<JobDetailResponse>> duplicate(@PathVariable UUID id) {
        JobDetailResponse response = jobService.duplicateJob(id, currentUserProvider.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Job duplicated", response));
    }

    @PutMapping("/{id}/assignment")
    @Operation(summary = "Assign the recruiter and/or hiring manager for a job")
    public ResponseEntity<ApiResponse<JobDetailResponse>> assign(@PathVariable UUID id, @RequestBody AssignJobRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Job assigned", jobService.assignJob(id, request, currentUserProvider.getCurrentUser())));
    }

    @GetMapping
    @Operation(summary = "Search/filter/sort/paginate jobs", description = "?title=&departmentId=&location=&status=&employmentType=&recruiterId=&hiringManagerId=&createdFrom=&createdTo=&page=&size=&sort=")
    public ResponseEntity<ApiResponse<PagedResponse<JobSummaryResponse>>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) UUID recruiterId,
            @RequestParam(required = false) UUID hiringManagerId,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        JobSearchCriteria criteria = JobSearchCriteria.builder()
                .title(title).departmentId(departmentId).location(location).status(status)
                .employmentType(employmentType).recruiterId(recruiterId).hiringManagerId(hiringManagerId)
                .createdFrom(createdFrom).createdTo(createdTo).build();

        return ResponseEntity.ok(ApiResponse.ok("OK", jobService.searchJobs(criteria, pageable, currentUserProvider.getCurrentUser())));
    }
}
