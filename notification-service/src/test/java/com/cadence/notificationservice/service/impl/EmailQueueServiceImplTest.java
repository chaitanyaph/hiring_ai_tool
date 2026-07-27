package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.dto.request.RetryBulkRequest;
import com.cadence.notificationservice.entity.EmailQueue;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.mapper.EmailQueueMapper;
import com.cadence.notificationservice.mapper.EmailQueueMapperImpl;
import com.cadence.notificationservice.repository.EmailAttachmentRepository;
import com.cadence.notificationservice.repository.EmailQueueRepository;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceImplTest {

    @Mock private EmailQueueRepository emailQueueRepository;
    @Mock private EmailAttachmentRepository emailAttachmentRepository;
    @Mock private NotificationTemplateRepository templateRepository;

    private final EmailQueueMapper emailQueueMapper = new EmailQueueMapperImpl();

    private EmailQueueServiceImpl emailQueueService;

    private UUID emailId;

    @BeforeEach
    void setUp() {
        emailQueueService = new EmailQueueServiceImpl(emailQueueRepository, emailAttachmentRepository, templateRepository, emailQueueMapper);
        emailId = UUID.randomUUID();
        lenient().when(emailQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(templateRepository.findAll()).thenReturn(List.of());
    }

    private EmailQueue failedEmail(int attempts) {
        return EmailQueue.builder().id(emailId).recipientEmail("x@mail.com").subject("s").bodyHtml("b")
                .status(EmailStatus.FAILED).attempts(attempts).maxAttempts(5).failureReason("SMTP error").build();
    }

    @Test
    void retry_shouldResetAttemptsAndStatusToPending() {
        when(emailQueueRepository.findById(emailId)).thenReturn(Optional.of(failedEmail(3)));

        emailQueueService.retry(emailId);

        verify(emailQueueRepository).save(argThat(e ->
                e.getStatus() == EmailStatus.PENDING && e.getAttempts() == 0 && e.getFailureReason() == null));
    }

    @Test
    void retry_shouldThrow_whenEmailNotFound() {
        when(emailQueueRepository.findById(emailId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailQueueService.retry(emailId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retryBulk_shouldResetAllSelectedEmails() {
        EmailQueue e1 = failedEmail(5);
        EmailQueue e2 = failedEmail(2);
        when(emailQueueRepository.findAllByIdIn(List.of(e1.getId(), e2.getId()))).thenReturn(List.of(e1, e2));

        emailQueueService.retryBulk(RetryBulkRequest.builder().emailQueueIds(List.of(e1.getId(), e2.getId())).build());

        verify(emailQueueRepository).saveAll(argThat((List<EmailQueue> list) ->
                list.stream().allMatch(e -> e.getStatus() == EmailStatus.PENDING && e.getAttempts() == 0)));
    }

    @Test
    void cancelScheduled_shouldSetCancelledStatus() {
        EmailQueue pending = EmailQueue.builder().id(emailId).recipientEmail("x@mail.com").subject("s").bodyHtml("b")
                .status(EmailStatus.PENDING).scheduledAt(LocalDateTime.now().plusHours(1)).build();
        when(emailQueueRepository.findById(emailId)).thenReturn(Optional.of(pending));

        emailQueueService.cancelScheduled(emailId);

        verify(emailQueueRepository).save(argThat(e -> e.getStatus() == EmailStatus.CANCELLED));
    }

    @Test
    void getDashboardStats_shouldComputeDeliveryRateFromCounts() {
        when(emailQueueRepository.countByStatusAndSentAtAfter(eq(EmailStatus.SENT), any())).thenReturn(2L);
        when(emailQueueRepository.countByStatusAndSentAtAfter(eq(EmailStatus.DELIVERED), any())).thenReturn(1L);
        when(emailQueueRepository.countByStatusAndSentAtAfter(eq(EmailStatus.OPENED), any())).thenReturn(0L);
        when(emailQueueRepository.countByStatus(EmailStatus.DELIVERED)).thenReturn(8L);
        when(emailQueueRepository.countByStatus(EmailStatus.OPENED)).thenReturn(2L);
        when(emailQueueRepository.countByStatus(EmailStatus.BOUNCED)).thenReturn(0L);
        when(emailQueueRepository.countByStatus(EmailStatus.FAILED)).thenReturn(0L);
        when(emailQueueRepository.countByStatus(EmailStatus.PENDING)).thenReturn(4L);

        var stats = emailQueueService.getDashboardStats();

        org.assertj.core.api.Assertions.assertThat(stats.getDeliveryRatePercent()).isEqualTo(100.0);
        org.assertj.core.api.Assertions.assertThat(stats.getScheduledUpcomingCount()).isEqualTo(4L);
    }
}
