package com.cadence.notificationservice.entity;

import com.cadence.notificationservice.constants.ColorTone;
import com.cadence.notificationservice.constants.NotificationCategory;
import com.cadence.notificationservice.constants.NotificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** In-app notification instance. Absorbs the suggested notification_status table -- status is a plain enum column. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "recipient_role", nullable = false, length = 40)
    private String recipientRole;

    @Column(name = "company_id")
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private NotificationCategory category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_tone", nullable = false, length = 20)
    @Builder.Default
    private ColorTone colorTone = ColorTone.INFO;

    @Column(name = "entity_type", length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

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
