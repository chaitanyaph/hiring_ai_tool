package com.cadence.companyservice.controller;

import com.cadence.companyservice.dto.request.CreateTeamInvitationRequest;
import com.cadence.companyservice.dto.request.UpdateTeamInvitationRequest;
import com.cadence.companyservice.dto.response.ApiResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.dto.response.TeamInvitationResponse;
import com.cadence.companyservice.service.TeamInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Team Invitations", description = "Invite lifecycle only -- Auth Service creates the login account on acceptance")
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;

    @PostMapping("/api/v1/companies/{companyId}/team-invitations")
    @Operation(summary = "Invite a teammate", description = "Publishes TeamInvitationCreated for Notification Service to email the invite link")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> createInvitation(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateTeamInvitationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        TeamInvitationResponse response = teamInvitationService.createInvitation(companyId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Invitation created", response));
    }

    @GetMapping("/api/v1/companies/{companyId}/team-invitations")
    @Operation(summary = "List invitations for a company", description = "Supports pagination and sorting, e.g. ?page=0&size=20&sort=createdAt,desc")
    public ResponseEntity<ApiResponse<PagedResponse<TeamInvitationResponse>>> listInvitations(
            @PathVariable UUID companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", teamInvitationService.listInvitations(companyId, pageable)));
    }

    @GetMapping("/api/v1/team-invitations/{id}")
    @Operation(summary = "Get an invitation by id")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> getInvitation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", teamInvitationService.getInvitation(id)));
    }

    @PutMapping("/api/v1/team-invitations/{id}")
    @Operation(summary = "Update a pending invitation's role or department")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> updateInvitation(
            @PathVariable UUID id,
            @RequestBody UpdateTeamInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Invitation updated", teamInvitationService.updateInvitation(id, request)));
    }

    @DeleteMapping("/api/v1/team-invitations/{id}")
    @Operation(summary = "Cancel a pending invitation")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(@PathVariable UUID id) {
        teamInvitationService.cancelInvitation(id);
        return ResponseEntity.ok(ApiResponse.ok("Invitation cancelled"));
    }

    @PostMapping("/api/v1/team-invitations/{token}/resend")
    @Operation(summary = "Resend an invitation", description = "Issues a fresh token and expiry, then republishes TeamInvitationCreated")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> resendInvitation(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok("Invitation resent", teamInvitationService.resendInvitation(token)));
    }
}
