package com.cadence.aiinterviewservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published on successful evaluation -- application-service is expected to consume this (and CandidateRecommendedEvent) to drive AI_INTERVIEW_PENDING -> AI_INTERVIEW_COMPLETED; no consumer exists there yet, see README "Architecture Decisions". */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewEvaluatedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private Integer overallScore;
    private LocalDateTime occurredAt;
}
