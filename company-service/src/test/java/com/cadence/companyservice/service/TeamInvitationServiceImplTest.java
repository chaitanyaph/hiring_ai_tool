package com.cadence.companyservice.service;

import com.cadence.companyservice.constant.InvitationStatus;
import com.cadence.companyservice.constant.TeamRole;
import com.cadence.companyservice.dto.request.CreateTeamInvitationRequest;
import com.cadence.companyservice.entity.TeamInvitation;
import com.cadence.companyservice.exception.DuplicatePendingInvitationException;
import com.cadence.companyservice.exception.InvalidInviteTokenException;
import com.cadence.companyservice.exception.InvitationNotPendingException;
import com.cadence.companyservice.kafka.producer.CompanyEventProducer;
import com.cadence.companyservice.mapper.TeamInvitationMapper;
import com.cadence.companyservice.repository.CompanyRepository;
import com.cadence.companyservice.repository.TeamInvitationRepository;
import com.cadence.companyservice.service.impl.TeamInvitationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamInvitationServiceImplTest {

    @Mock private TeamInvitationRepository invitationRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private TeamInvitationMapper invitationMapper;
    @Mock private CompanyEventProducer eventProducer;

    @InjectMocks
    private TeamInvitationServiceImpl invitationService;

    @Test
    void createInvitation_shouldThrow_whenPendingInvitationAlreadyExistsForEmail() {
        ReflectionTestUtils.setField(invitationService, "expiryDays", 7L);
        UUID companyId = UUID.randomUUID();
        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(companyId, "vikram@acme.com", InvitationStatus.PENDING))
                .thenReturn(true);

        CreateTeamInvitationRequest request = CreateTeamInvitationRequest.builder()
                .email("vikram@acme.com").role(TeamRole.HR_RECRUITER).build();

        assertThatThrownBy(() -> invitationService.createInvitation(companyId, request, "admin"))
                .isInstanceOf(DuplicatePendingInvitationException.class);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void createInvitation_shouldGenerateTokenAndExpiry_andPublishEvent() {
        ReflectionTestUtils.setField(invitationService, "expiryDays", 7L);
        UUID companyId = UUID.randomUUID();
        when(companyRepository.existsById(companyId)).thenReturn(true);
        when(invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(any(), any(), any())).thenReturn(false);
        when(invitationRepository.save(any(TeamInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTeamInvitationRequest request = CreateTeamInvitationRequest.builder()
                .email("Vikram@Acme.com").role(TeamRole.HR_RECRUITER).build();
        invitationService.createInvitation(companyId, request, "admin");

        ArgumentCaptor<TeamInvitation> captor = ArgumentCaptor.forClass(TeamInvitation.class);
        verify(invitationRepository).save(captor.capture());
        TeamInvitation saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("vikram@acme.com");
        assertThat(saved.getInviteToken()).isNotBlank();
        assertThat(saved.getStatus()).isEqualTo(InvitationStatus.PENDING);
        assertThat(saved.getExpiryDate()).isAfter(LocalDateTime.now());
        verify(eventProducer).publishTeamInvitationCreated(any());
    }

    @Test
    void cancelInvitation_shouldThrow_whenAlreadyAccepted() {
        UUID invitationId = UUID.randomUUID();
        TeamInvitation invitation = TeamInvitation.builder().id(invitationId).status(InvitationStatus.ACCEPTED).build();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.cancelInvitation(invitationId))
                .isInstanceOf(InvitationNotPendingException.class);
    }

    @Test
    void resendInvitation_shouldRegenerateTokenAndExpiry_evenIfOriginalExpired() {
        ReflectionTestUtils.setField(invitationService, "expiryDays", 7L);
        String oldToken = UUID.randomUUID().toString();
        TeamInvitation invitation = TeamInvitation.builder().id(UUID.randomUUID())
                .inviteToken(oldToken).status(InvitationStatus.PENDING)
                .expiryDate(LocalDateTime.now().minusDays(1)).build();
        when(invitationRepository.findByInviteToken(oldToken)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(TeamInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        invitationService.resendInvitation(oldToken);

        ArgumentCaptor<TeamInvitation> captor = ArgumentCaptor.forClass(TeamInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getInviteToken()).isNotEqualTo(oldToken);
        assertThat(captor.getValue().getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    void resendInvitation_shouldThrow_whenTokenUnknown() {
        when(invitationRepository.findByInviteToken("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.resendInvitation("bogus"))
                .isInstanceOf(InvalidInviteTokenException.class);
    }

    @Test
    void markAcceptedByToken_shouldBeIdempotent_whenAlreadyAccepted() {
        String token = UUID.randomUUID().toString();
        TeamInvitation invitation = TeamInvitation.builder().id(UUID.randomUUID())
                .inviteToken(token).status(InvitationStatus.ACCEPTED).build();
        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));

        invitationService.markAcceptedByToken(token);

        verify(invitationRepository, never()).save(any());
    }

    @Test
    void markAcceptedByToken_shouldTransitionPendingToAccepted() {
        String token = UUID.randomUUID().toString();
        TeamInvitation invitation = TeamInvitation.builder().id(UUID.randomUUID())
                .inviteToken(token).status(InvitationStatus.PENDING).build();
        when(invitationRepository.findByInviteToken(token)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(TeamInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        invitationService.markAcceptedByToken(token);

        ArgumentCaptor<TeamInvitation> captor = ArgumentCaptor.forClass(TeamInvitation.class);
        verify(invitationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(captor.getValue().getAcceptedAt()).isNotNull();
    }
}
