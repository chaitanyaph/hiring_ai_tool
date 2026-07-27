package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.ColorTone;
import com.cadence.notificationservice.constants.NotificationCategory;
import com.cadence.notificationservice.constants.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationCategory category;
    private String title;
    private String message;
    private ColorTone colorTone;
    private String entityType;
    private UUID entityId;
    private NotificationStatus status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
