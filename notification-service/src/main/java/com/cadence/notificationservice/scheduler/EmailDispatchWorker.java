package com.cadence.notificationservice.scheduler;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.email.EmailSender;
import com.cadence.notificationservice.entity.EmailAttachment;
import com.cadence.notificationservice.entity.EmailQueue;
import com.cadence.notificationservice.repository.EmailAttachmentRepository;
import com.cadence.notificationservice.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Polls PENDING (immediate) and due-FAILED (exponential backoff, §13)
 * email_queue rows and sends them via Spring Mail. Delivered/Opened/
 * Bounced status transitions would come from a real ESP webhook, which
 * doesn't exist for this build (no email provider account is wired
 * up) -- a successful send is marked SENT only, not faked further up
 * the funnel; this is flagged in the README rather than silently
 * fabricating delivery tracking.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatchWorker {

    private final EmailQueueRepository emailQueueRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailSender emailSender;

    @Value("${notification.retry.backoff-minutes}")
    private String backoffMinutesCsv;

    @Scheduled(fixedRateString = "${notification.dispatch.fixed-rate-ms}")
    public void dispatchDueEmails() {
        List<EmailQueue> due = new ArrayList<>();
        due.addAll(emailQueueRepository.findAllByStatusAndScheduledAtIsNull(EmailStatus.PENDING));
        due.addAll(emailQueueRepository.findAllByStatusAndNextRetryAtLessThanEqual(EmailStatus.FAILED, LocalDateTime.now()));
        due.forEach(this::send);
    }

    private void send(EmailQueue email) {
        try {
            List<EmailAttachment> attachments = emailAttachmentRepository.findAllByEmailQueueId(email.getId());
            emailSender.send(email.getRecipientEmail(), email.getSubject(), email.getBodyHtml(), attachments);
            email.setStatus(EmailStatus.SENT);
            email.setSentAt(LocalDateTime.now());
            email.setFailureReason(null);
            email.setNextRetryAt(null);
        } catch (Exception e) {
            int attempts = email.getAttempts() + 1;
            email.setAttempts(attempts);
            email.setFailureReason(e.getMessage());
            email.setStatus(EmailStatus.FAILED);
            if (attempts < email.getMaxAttempts()) {
                email.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes(attempts)));
            } else {
                email.setNextRetryAt(null);
            }
            log.warn("Email send failed for {} (attempt {}/{}): {}", email.getRecipientEmail(), attempts, email.getMaxAttempts(), e.getMessage());
        }
        email.setUpdatedAt(LocalDateTime.now());
        emailQueueRepository.save(email);
    }

    private long backoffMinutes(int attempt) {
        String[] parts = backoffMinutesCsv.split(",");
        int idx = Math.min(attempt - 1, parts.length - 1);
        return Long.parseLong(parts[idx].trim());
    }
}
