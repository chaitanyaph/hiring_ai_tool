package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.entity.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, UUID> {
    List<EmailAttachment> findAllByEmailQueueId(UUID emailQueueId);
}
