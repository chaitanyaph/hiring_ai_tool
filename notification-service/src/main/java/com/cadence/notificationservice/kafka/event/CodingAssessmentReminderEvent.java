package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentReminderEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String assessmentLink;
    private LocalDateTime expiresAt;
    private LocalDateTime occurredAt;
}
