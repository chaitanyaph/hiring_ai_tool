package com.cadence.interviewmanagementservice.controller;

import com.cadence.interviewmanagementservice.dto.request.CreateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.request.UpdateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.response.ApiResponse;
import com.cadence.interviewmanagementservice.dto.response.InterviewRoundResponse;
import com.cadence.interviewmanagementservice.security.CurrentUserProvider;
import com.cadence.interviewmanagementservice.service.InterviewRoundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Module 1: per-company interview round configuration. Not directly pixel-mocked in the Figma (round-type list is currently static) -- this is the API that would make the schedule modal's round list dynamic per company. */
@RestController
@RequestMapping("/api/v1/recruiter/interview-rounds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Interview Rounds", description = "Per-company interview round configuration (Technical/Manager/Architect/HR/Custom)")
public class RecruiterInterviewRoundController {

    private final InterviewRoundService interviewRoundService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Create an interview round")
    public ResponseEntity<ApiResponse<InterviewRoundResponse>> create(@Valid @RequestBody CreateInterviewRoundRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Interview round created", interviewRoundService.createRound(companyId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update an interview round")
    public ResponseEntity<ApiResponse<InterviewRoundResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateInterviewRoundRequest request) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("Interview round updated", interviewRoundService.updateRound(companyId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete an interview round")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        interviewRoundService.deleteRound(companyId, id);
        return ResponseEntity.ok(ApiResponse.ok("Interview round deleted"));
    }

    @GetMapping
    @Operation(summary = "List interview rounds for this company")
    public ResponseEntity<ApiResponse<List<InterviewRoundResponse>>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return ResponseEntity.ok(ApiResponse.ok("OK", interviewRoundService.listRounds(companyId, activeOnly)));
    }
}
