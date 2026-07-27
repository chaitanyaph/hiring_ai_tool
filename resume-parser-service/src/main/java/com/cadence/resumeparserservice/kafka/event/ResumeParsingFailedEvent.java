package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published when the pipeline fails at any checkpoint -- forward-looking scaffolding for a future recruiter-alert consumer in Notification Service. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeParsingFailedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private String reason;
    private LocalDateTime occurredAt;
}
