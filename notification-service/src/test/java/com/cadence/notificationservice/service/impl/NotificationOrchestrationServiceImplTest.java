package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.*;
import com.cadence.notificationservice.email.TemplateRenderer;
import com.cadence.notificationservice.entity.EmailQueue;
import com.cadence.notificationservice.entity.NotificationTemplate;
import com.cadence.notificationservice.feign.ApplicationServiceClient;
import com.cadence.notificationservice.feign.CandidateServiceClient;
import com.cadence.notificationservice.feign.CompanyServiceClient;
import com.cadence.notificationservice.feign.InterviewManagementServiceClient;
import com.cadence.notificationservice.feign.dto.ApplicationSummaryDto;
import com.cadence.notificationservice.feign.dto.CandidateDto;
import com.cadence.notificationservice.feign.dto.FeignApiResponse;
import com.cadence.notificationservice.kafka.event.*;
import com.cadence.notificationservice.repository.EmailQueueRepository;
import com.cadence.notificationservice.repository.NotificationLogRepository;
import com.cadence.notificationservice.repository.NotificationRepository;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestrationServiceImplTest {

    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private EmailQueueRepository emailQueueRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private CandidateServiceClient candidateServiceClient;
    @Mock private CompanyServiceClient companyServiceClient;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private InterviewManagementServiceClient interviewManagementServiceClient;

    private NotificationOrchestrationServiceImpl orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new NotificationOrchestrationServiceImpl(
                templateRepository, emailQueueRepository, notificationRepository, notificationLogRepository,
                new TemplateRenderer(), candidateServiceClient, companyServiceClient, applicationServiceClient,
                interviewManagementServiceClient);
        lenient().when(emailQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private NotificationTemplate template(TemplateCategory category, String subject, String body) {
        return NotificationTemplate.builder().id(UUID.randomUUID()).category(category)
                .triggerEvent(TriggerEvent.NONE).name(category.name()).subject(subject).bodyHtml(body).active(true).build();
    }

    @Test
    void handleTeamInvitationCreated_shouldQueueRecruiterInvitationEmail() {
        when(templateRepository.findByCategory(TemplateCategory.RECRUITER_INVITATION))
                .thenReturn(Optional.of(template(TemplateCategory.RECRUITER_INVITATION, "Join {{company_name}}", "<p>Hi {{recipient_name}}</p>")));
        when(companyServiceClient.getCompany(any())).thenThrow(new RuntimeException("unreachable"));

        orchestrationService.handleTeamInvitationCreated(TeamInvitationCreatedEvent.builder()
                .invitationId(UUID.randomUUID()).companyId(UUID.randomUUID()).email("new@company.com")
                .firstName("Ananya").role("HR_RECRUITER").inviteToken("tok-123").build());

        verify(emailQueueRepository).save(argThat(e -> e.getRecipientEmail().equals("new@company.com") && e.getSubject().contains("Join")));
    }

    @Test
    void handleUserRegistered_shouldQueueRegistrationAndVerificationEmails_whenCandidate() {
        when(templateRepository.findByCategory(TemplateCategory.CANDIDATE_REGISTRATION))
                .thenReturn(Optional.of(template(TemplateCategory.CANDIDATE_REGISTRATION, "Welcome {{candidate_name}}", "<p>Hi</p>")));
        when(templateRepository.findByCategory(TemplateCategory.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(template(TemplateCategory.EMAIL_VERIFICATION, "Verify", "<p>{{verification_link}}</p>")));

        orchestrationService.handleUserRegistered(UserRegisteredEvent.builder()
                .userId(UUID.randomUUID()).fullName("Rohan Mehta").email("rohan@mail.com").userType("CANDIDATE")
                .verificationLink("https://verify/xyz").build());

        verify(emailQueueRepository, times(2)).save(any(EmailQueue.class));
        verify(notificationRepository).save(argThat(n -> n.getCategory() == NotificationCategory.ACCOUNT));
    }

    @Test
    void handleApplicationCreated_shouldQueueApplicationReceivedEmail_whenEnrichmentSucceeds() {
        UUID applicationId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        ApplicationSummaryDto summary = new ApplicationSummaryDto();
        summary.setId(applicationId);
        summary.setCandidateNameSnapshot("Sneha Iyer");
        summary.setCandidateEmailSnapshot("sneha@mail.com");
        summary.setJobTitleSnapshot("Backend Engineer");
        when(applicationServiceClient.getApplicationsByJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", List.of(summary)));
        when(companyServiceClient.getCompany(any())).thenThrow(new RuntimeException("unreachable"));
        when(templateRepository.findByCategory(TemplateCategory.APPLICATION_RECEIVED))
                .thenReturn(Optional.of(template(TemplateCategory.APPLICATION_RECEIVED, "Received {{job_title}}", "<p>Hi {{candidate_name}}</p>")));

        orchestrationService.handleApplicationCreated(ApplicationCreatedEvent.builder()
                .applicationId(applicationId).companyId(UUID.randomUUID()).jobId(jobId).candidateId(candidateId).build());

        verify(emailQueueRepository).save(argThat(e -> e.getRecipientEmail().equals("sneha@mail.com")));
        verify(notificationRepository).save(argThat(n -> n.getRecipientId().equals(candidateId)));
    }

    @Test
    void handleApplicationCreated_shouldSkipAndLog_whenCandidateEmailUnresolvable() {
        UUID jobId = UUID.randomUUID();
        when(applicationServiceClient.getApplicationsByJob(jobId)).thenThrow(new RuntimeException("feign down"));
        when(candidateServiceClient.getCandidateSummary(any())).thenThrow(new RuntimeException("feign down"));

        orchestrationService.handleApplicationCreated(ApplicationCreatedEvent.builder()
                .applicationId(UUID.randomUUID()).companyId(UUID.randomUUID()).jobId(jobId).candidateId(UUID.randomUUID()).build());

        verifyNoInteractions(emailQueueRepository);
        verify(notificationLogRepository).save(argThat(l -> l.getLevel() == LogLevel.WARN));
    }

    @Test
    void handleInterviewRescheduled_shouldRecoverRecipientFromPriorScheduledEmail() {
        UUID interviewId = UUID.randomUUID();
        EmailQueue priorEmail = EmailQueue.builder().recipientEmail("candidate@mail.com").recipientName("Arjun Verma")
                .relatedEntityType("INTERVIEW").relatedEntityId(interviewId).build();
        when(emailQueueRepository.findFirstByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc("INTERVIEW", interviewId))
                .thenReturn(Optional.of(priorEmail));
        when(templateRepository.findByCategory(TemplateCategory.INTERVIEW_RESCHEDULED))
                .thenReturn(Optional.of(template(TemplateCategory.INTERVIEW_RESCHEDULED, "Rescheduled", "<p>Hi {{candidate_name}}</p>")));

        orchestrationService.handleInterviewRescheduled(InterviewRescheduledEvent.builder()
                .interviewId(interviewId).applicationId(UUID.randomUUID())
                .newScheduledDate(LocalDate.now().plusDays(1)).newScheduledTime(LocalTime.of(11, 0)).build());

        verify(emailQueueRepository).save(argThat(e -> e.getRecipientEmail().equals("candidate@mail.com")));
    }

    @Test
    void handleInterviewRescheduled_shouldLogWarning_whenNoPriorEmailExists() {
        UUID interviewId = UUID.randomUUID();
        when(emailQueueRepository.findFirstByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc("INTERVIEW", interviewId))
                .thenReturn(Optional.empty());

        orchestrationService.handleInterviewRescheduled(InterviewRescheduledEvent.builder()
                .interviewId(interviewId).applicationId(UUID.randomUUID()).build());

        verify(notificationLogRepository).save(argThat(l -> l.getLevel() == LogLevel.WARN));
        verifyNoMoreInteractions(emailQueueRepository);
    }

    @Test
    void handleCandidateShortlisted_shouldPickRejectedTemplate_whenDecisionIsReject() {
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(candidateServiceClient.getCandidateSummary(candidateId))
                .thenReturn(new FeignApiResponse<>(true, "OK", new CandidateDto(candidateId, "Karthik Rao", "karthik@mail.com")));
        when(templateRepository.findByCategory(TemplateCategory.RESUME_REJECTED))
                .thenReturn(Optional.of(template(TemplateCategory.RESUME_REJECTED, "Update", "<p>Hi {{candidate_name}}</p>")));

        orchestrationService.handleCandidateShortlisted(CandidateShortlistedEvent.builder()
                .applicationId(applicationId).jobId(UUID.randomUUID()).candidateId(candidateId).decision("REJECT").build());

        verify(templateRepository).findByCategory(TemplateCategory.RESUME_REJECTED);
        verify(emailQueueRepository).save(any(EmailQueue.class));
    }
}
