package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewEvaluatedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private Integer overallScore;
    private LocalDateTime occurredAt;
}
