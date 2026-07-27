package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.EmailStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Backs drawer-email-history (§A3): recipient/subject/body + delivery timeline. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailQueueDetailResponse {
    private UUID id;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String bodyHtml;
    private EmailStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private String failureReason;
    private List<AttachmentInfo> attachments;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentInfo {
        private UUID id;
        private String fileName;
        private String contentType;
        private long sizeBytes;
    }
}
