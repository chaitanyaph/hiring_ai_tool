package com.cadence.applicationservice.controller;

import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.PlatformRole;
import com.cadence.applicationservice.constant.Priority;
import com.cadence.applicationservice.dto.request.*;
import com.cadence.applicationservice.dto.response.*;
import com.cadence.applicationservice.security.CurrentUser;
import com.cadence.applicationservice.security.CurrentUserProvider;
import com.cadence.applicationservice.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * One controller for both audiences (candidate self-service and
 * recruiter dashboard), matching how the product spec itself groups
 * them under the same /applications resource -- each endpoint is
 * individually role-gated, and the two "shared" endpoints (detail,
 * timeline, history) branch internally by caller role rather than
 * needing two near-duplicate controllers.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "The application lifecycle -- apply, track, and the recruiter pipeline dashboard")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final CurrentUserProvider currentUserProvider;

    // ---- Candidate-facing ----

    @PostMapping
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply to a job", description = "Validates: not already applied, job PUBLISHED and deadline not passed, candidate profile complete and resume uploaded")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(@Valid @RequestBody ApplyRequest request) {
        ApplicationResponse response = applicationService.apply(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Application submitted", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "List my applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listMyApplications() {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.listMyApplications(currentUserProvider.getCurrentUser())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Withdraw an application")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Application withdrawn", applicationService.withdraw(currentUserProvider.getCurrentUser(), id)));
    }

    @PostMapping("/{id}/accept-offer")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Accept a released offer", description = "Only valid while status is OFFER_RELEASED; moves straight to HIRED")
    public ResponseEntity<ApiResponse<ApplicationResponse>> acceptOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Offer accepted", applicationService.acceptOffer(currentUserProvider.getCurrentUser(), id)));
    }

    @PostMapping("/{id}/reject-offer")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Decline a released offer")
    public ResponseEntity<ApiResponse<ApplicationResponse>> rejectOffer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Offer declined", applicationService.rejectOffer(currentUserProvider.getCurrentUser(), id)));
    }

    // ---- Recruiter-facing ----

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Search/filter/sort/paginate all applications for a company",
            description = "?jobId=&recruiterId=&hiringManagerId=&status=&stage=&priority=&candidateName=&candidateEmail=&jobTitle=&minOverallScore=&appliedFrom=&appliedTo=&page=&size=&sort=")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getCompanyApplications(
            @PathVariable UUID companyId,
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) UUID recruiterId,
            @RequestParam(required = false) UUID hiringManagerId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) ApplicationStage stage,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String candidateName,
            @RequestParam(required = false) String candidateEmail,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) Integer minOverallScore,
            @RequestParam(required = false) LocalDateTime appliedFrom,
            @RequestParam(required = false) LocalDateTime appliedTo,
            @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {

        CurrentUser recruiter = currentUserProvider.getCurrentUser();
        if (recruiter.getCompanyId() == null || !recruiter.getCompanyId().equals(companyId)) {
            throw new AccessDeniedException("You may only view applications for your own company");
        }

        ApplicationSearchCriteria criteria = ApplicationSearchCriteria.builder()
                .jobId(jobId).recruiterId(recruiterId).hiringManagerId(hiringManagerId)
                .status(status).stage(stage).priority(priority)
                .candidateName(candidateName).candidateEmail(candidateEmail).jobTitle(jobTitle)
                .minOverallScore(minOverallScore).appliedFrom(appliedFrom).appliedTo(appliedTo).build();

        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.searchCompanyApplications(recruiter, criteria, pageable)));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "List applications for one job")
    public ResponseEntity<ApiResponse<PagedResponse<ApplicationResponse>>> getJobApplications(
            @PathVariable UUID jobId, @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.getJobApplications(currentUserProvider.getCurrentUser(), jobId, pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Change status", description = "Validates the lifecycle state machine; Hiring Manager needs the APPLICATION_EDIT permission")
    public ResponseEntity<ApiResponse<ApplicationResponse>> changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated", applicationService.changeStatus(currentUserProvider.getCurrentUser(), id, request)));
    }

    @PutMapping("/{id}/assign-recruiter")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Assign the recruiter for this application")
    public ResponseEntity<ApiResponse<ApplicationResponse>> assignRecruiter(@PathVariable UUID id, @Valid @RequestBody AssignRecruiterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Recruiter assigned", applicationService.assignRecruiter(currentUserProvider.getCurrentUser(), id, request)));
    }

    @PutMapping("/{id}/assign-hiring-manager")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Assign the hiring manager for this application")
    public ResponseEntity<ApiResponse<ApplicationResponse>> assignHiringManager(@PathVariable UUID id, @Valid @RequestBody AssignHiringManagerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Hiring manager assigned", applicationService.assignHiringManager(currentUserProvider.getCurrentUser(), id, request)));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Add an internal recruiter note", description = "Never visible to the candidate")
    public ResponseEntity<ApiResponse<NoteResponse>> addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest request) {
        NoteResponse response = applicationService.addNote(currentUserProvider.getCurrentUser(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Note added", response));
    }

    // ---- Shared: candidate (own application) or recruiter (own company) ----

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE','COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Get application detail")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getDetail(@PathVariable UUID id) {
        CurrentUser caller = currentUserProvider.getCurrentUser();
        ApplicationResponse response = PlatformRole.CANDIDATE.equals(caller.getRole())
                ? applicationService.getForCandidate(caller, id)
                : applicationService.getForRecruiter(caller, id);
        return ResponseEntity.ok(ApiResponse.ok("OK", response));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("hasAnyRole('CANDIDATE','COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Unified chronological timeline (status + stage changes merged)")
    public ResponseEntity<ApiResponse<List<TimelineEntryResponse>>> getTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.getTimeline(currentUserProvider.getCurrentUser(), id)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('CANDIDATE','COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER')")
    @Operation(summary = "Raw structured status/stage history")
    public ResponseEntity<ApiResponse<ApplicationHistoryResponse>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", applicationService.getHistory(currentUserProvider.getCurrentUser(), id)));
    }
}
