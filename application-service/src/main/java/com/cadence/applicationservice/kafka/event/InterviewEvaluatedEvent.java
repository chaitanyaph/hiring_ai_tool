package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed -- published by ai-interview-service on topic
 * ai-interview.interview.evaluated once a completed AI interview session
 * has actually been scored. Field names must match that event's JSON
 * shape exactly (overallScore, not score) -- Jackson binds by property
 * name and a mismatch here silently deserializes to null.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewEvaluatedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private Integer overallScore;
    private LocalDateTime occurredAt;
}
