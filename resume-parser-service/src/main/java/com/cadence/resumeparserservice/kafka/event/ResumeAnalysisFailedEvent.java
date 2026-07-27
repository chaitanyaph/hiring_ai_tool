package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published when match analysis fails at any checkpoint. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalysisFailedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String reason;
    private LocalDateTime occurredAt;
}
