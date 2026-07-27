package com.cadence.companyservice.service.impl;

import com.cadence.companyservice.constant.InvitationStatus;
import com.cadence.companyservice.dto.request.CreateTeamInvitationRequest;
import com.cadence.companyservice.dto.request.UpdateTeamInvitationRequest;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.dto.response.TeamInvitationResponse;
import com.cadence.companyservice.entity.TeamInvitation;
import com.cadence.companyservice.exception.*;
import com.cadence.companyservice.kafka.event.TeamInvitationCancelledEvent;
import com.cadence.companyservice.kafka.event.TeamInvitationCreatedEvent;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.TeamInvitationMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.TeamInvitationRepository;
import com.cadence.companyservice.service.TeamInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamInvitationServiceImpl implements TeamInvitationService {

    private final TeamInvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final TeamInvitationMapper invitationMapper;
    private final CompanyEventProducer eventProducer;

    @Value("${app.invitation.expiry-days:7}")
    private long expiryDays;

    @Override
    @Transactional
    public TeamInvitationResponse createInvitation(UUID companyId, CreateTeamInvitationRequest request, String actor) {
        ensureCompanyExists(companyId);
        String email = request.getEmail().trim().toLowerCase();

        if (invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(companyId, email, InvitationStatus.PENDING)) {
            throw new DuplicatePendingInvitationException(email);
        }

        TeamInvitation invitation = TeamInvitation.builder()
                .companyId(companyId)
                .departmentId(request.getDepartmentId())
                .email(email)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .inviteToken(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(expiryDays))
                .createdBy(actor)
                .build();
        invitation = invitationRepository.save(invitation);
        log.info("Team invitation created for {} ({}) at company {}", email, invitation.getRole(), companyId);

        publishCreated(invitation);
        return invitationMapper.toResponse(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TeamInvitationResponse> listInvitations(UUID companyId, Pageable pageable) {
        ensureCompanyExists(companyId);
        Page<TeamInvitationResponse> page = invitationRepository.findAllByCompanyId(companyId, pageable)
                .map(invitationMapper::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamInvitationResponse getInvitation(UUID invitationId) {
        return invitationMapper.toResponse(findInvitationOrThrow(invitationId));
    }

    @Override
    @Transactional
    public TeamInvitationResponse updateInvitation(UUID invitationId, UpdateTeamInvitationRequest request) {
        TeamInvitation invitation = findInvitationOrThrow(invitationId);
        ensurePending(invitation);

        if (request.getDepartmentId() != null) {
            invitation.setDepartmentId(request.getDepartmentId());
        }
        if (request.getRole() != null) {
            invitation.setRole(request.getRole());
        }
        invitation = invitationRepository.save(invitation);
        return invitationMapper.toResponse(invitation);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId) {
        TeamInvitation invitation = findInvitationOrThrow(invitationId);
        ensureStatusPending(invitation);

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);

        eventProducer.publishTeamInvitationCancelled(TeamInvitationCancelledEvent.builder()
                .invitationId(invitation.getId()).companyId(invitation.getCompanyId())
                .email(invitation.getEmail()).occurredAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional
    public TeamInvitationResponse resendInvitation(String inviteToken) {
        // Deliberately checks status only, not expiry: resending is exactly
        // how an expired-but-still-PENDING invitation gets revived with a
        // fresh token and expiry date.
        TeamInvitation invitation = invitationRepository.findByInviteToken(inviteToken)
                .orElseThrow(InvalidInviteTokenException::new);
        ensureStatusPending(invitation);

        invitation.setInviteToken(UUID.randomUUID().toString());
        invitation.setExpiryDate(LocalDateTime.now().plusDays(expiryDays));
        invitation = invitationRepository.save(invitation);

        publishCreated(invitation);
        return invitationMapper.toResponse(invitation);
    }

    @Override
    @Transactional
    public void markAcceptedByToken(String inviteToken) {
        invitationRepository.findByInviteToken(inviteToken).ifPresent(this::markAccepted);
    }

    @Override
    @Transactional
    public void markAcceptedByEmail(UUID companyId, String email) {
        invitationRepository.findFirstByCompanyIdAndEmailIgnoreCaseAndStatus(companyId, email.toLowerCase(), InvitationStatus.PENDING)
                .ifPresent(this::markAccepted);
    }

    private void markAccepted(TeamInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            return;
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        log.info("Invitation accepted: {} ({})", invitation.getEmail(), invitation.getId());
    }

    private void publishCreated(TeamInvitation invitation) {
        eventProducer.publishTeamInvitationCreated(TeamInvitationCreatedEvent.builder()
                .invitationId(invitation.getId()).companyId(invitation.getCompanyId())
                .email(invitation.getEmail()).firstName(invitation.getFirstName()).role(invitation.getRole())
                .inviteToken(invitation.getInviteToken()).expiryDate(invitation.getExpiryDate())
                .occurredAt(LocalDateTime.now()).build());
    }

    private void ensurePending(TeamInvitation invitation) {
        ensureStatusPending(invitation);
        if (invitation.isExpired()) {
            throw new InvitationExpiredException();
        }
    }

    private void ensureStatusPending(TeamInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotPendingException(invitation.getStatus().name());
        }
    }

    private TeamInvitation findInvitationOrThrow(UUID invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEAM_INVITATION_NOT_FOUND, "Team invitation not found: " + invitationId));
    }

    private void ensureCompanyExists(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.COMPANY_NOT_FOUND, "Company not found: " + companyId);
        }
    }
}
