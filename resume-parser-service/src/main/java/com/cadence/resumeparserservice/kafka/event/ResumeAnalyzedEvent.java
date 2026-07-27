package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published on successful match analysis -- forward-looking scaffolding for the not-yet-built Shortlisting service. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalyzedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeMatchId;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
