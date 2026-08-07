package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInterviewExpiredEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
