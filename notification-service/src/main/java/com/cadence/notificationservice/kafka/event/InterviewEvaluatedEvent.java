package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed -- published by ai-interview-service on topic ai-interview.interview.evaluated.
 * hiringRecommendation is a String mirror of ai-interview-service's own HiringRecommendation
 * enum (PROCEED/HOLD/REJECT) -- deserialized loosely since that enum lives in a sibling service.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewEvaluatedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private Integer overallScore;
    private String hiringRecommendation;
    private LocalDateTime occurredAt;
}
