package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodeSubmittedEvent {
    private UUID submissionId;
    private UUID candidateAssessmentId;
    private UUID questionId;
    private UUID applicationId;
    private LocalDateTime occurredAt;
}
