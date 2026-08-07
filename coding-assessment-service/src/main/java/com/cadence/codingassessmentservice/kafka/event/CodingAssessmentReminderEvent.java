package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published once by the reminder sweep, for a NOT_STARTED attempt approaching its expiresAt deadline. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentReminderEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String assessmentLink;
    private LocalDateTime expiresAt;
    private LocalDateTime occurredAt;
}
