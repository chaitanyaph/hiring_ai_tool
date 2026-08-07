package com.cadence.aiinterviewservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published once by the reminder sweep, for a NOT_STARTED session approaching its expiresAt deadline. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInterviewReminderEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String interviewLink;
    private LocalDateTime expiresAt;
    private LocalDateTime occurredAt;
}
