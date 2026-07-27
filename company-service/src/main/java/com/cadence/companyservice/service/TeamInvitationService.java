package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.CreateTeamInvitationRequest;
import com.cadence.companyservice.dto.request.UpdateTeamInvitationRequest;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.dto.response.TeamInvitationResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TeamInvitationService {
    TeamInvitationResponse createInvitation(UUID companyId, CreateTeamInvitationRequest request, String actor);
    PagedResponse<TeamInvitationResponse> listInvitations(UUID companyId, Pageable pageable);
    TeamInvitationResponse getInvitation(UUID invitationId);
    TeamInvitationResponse updateInvitation(UUID invitationId, UpdateTeamInvitationRequest request);
    void cancelInvitation(UUID invitationId);
    TeamInvitationResponse resendInvitation(String inviteToken);

    void markAcceptedByToken(String inviteToken);
    void markAcceptedByEmail(UUID companyId, String email);
}
