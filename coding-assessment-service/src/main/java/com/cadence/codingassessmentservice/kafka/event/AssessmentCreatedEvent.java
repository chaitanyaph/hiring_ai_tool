package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentCreatedEvent {
    private UUID assessmentId;
    private UUID jobId;
    private UUID companyId;
    private LocalDateTime occurredAt;
}
