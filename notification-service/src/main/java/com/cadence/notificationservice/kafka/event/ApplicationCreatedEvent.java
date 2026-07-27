package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationCreatedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeId;
    private LocalDateTime occurredAt;
}
