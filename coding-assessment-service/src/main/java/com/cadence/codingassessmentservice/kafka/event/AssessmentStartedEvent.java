package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentStartedEvent {
    private UUID candidateAssessmentId;
    private UUID assessmentId;
    private UUID applicationId;
    private LocalDateTime occurredAt;
}
