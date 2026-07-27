package com.cadence.aiinterviewservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors Resume Parser Service's own ResumeAnalyzedEvent field-for-field -- the trigger for AI Shortlisting. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeAnalyzedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeMatchId;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
