package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.entity.EmailQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {

    Page<EmailQueue> findAllByStatus(EmailStatus status, Pageable pageable);

    /**
     * Reused as an implicit recipient-resolution cache: Interview
     * Rescheduled/Cancelled Kafka events only carry interviewId (no
     * candidateId/email), so the recipient for those follow-up emails
     * is recovered from the original "interview scheduled" email
     * already queued for the same relatedEntityId -- no separate
     * cache table needed.
     */
    Optional<EmailQueue> findFirstByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(String relatedEntityType, UUID relatedEntityId);

    Page<EmailQueue> findAllByStatusAndRecipientEmailContainingIgnoreCase(EmailStatus status, String recipientEmail, Pageable pageable);

    Page<EmailQueue> findAllByRecipientEmailContainingIgnoreCase(String recipientEmail, Pageable pageable);

    List<EmailQueue> findAllByStatusAndNextRetryAtLessThanEqual(EmailStatus status, LocalDateTime now);

    List<EmailQueue> findAllByStatusAndScheduledAtIsNull(EmailStatus status);

    long countByStatus(EmailStatus status);

    long countByStatusAndSentAtAfter(EmailStatus status, LocalDateTime after);

    long countByTemplateIdAndStatusIn(UUID templateId, List<EmailStatus> statuses);

    List<EmailQueue> findAllByIdIn(List<UUID> ids);
}
