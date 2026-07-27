package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalyzedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeMatchId;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
