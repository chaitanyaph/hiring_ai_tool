package com.cadence.notificationservice.entity;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.constants.TriggerEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single status-machine row per email -- absorbs the suggested
 * notification_history + notification_delivery + the rest of
 * notification_status. Queue/History/Scheduled/Failed views (Figma
 * §A3) are all just different filters over this one table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email_queue")
public class EmailQueue {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "LONGTEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "related_entity_type", length = 30)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", length = 50)
    private TriggerEvent triggerEvent;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
