package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.EmailStatus;
import com.cadence.notificationservice.constants.TemplateCategory;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the Figma's Email History / Scheduled / Failed table rows (§A3). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailQueueItemResponse {
    private UUID id;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private TemplateCategory templateCategory;
    private EmailStatus status;
    private int attempts;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private String failureReason;
    private LocalDateTime createdAt;
}
