package com.cadence.aiinterviewservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published by the expiry sweep the moment a NOT_STARTED session is flipped to EXPIRED. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInterviewExpiredEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
