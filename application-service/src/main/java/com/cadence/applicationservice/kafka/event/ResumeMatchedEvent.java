package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/**
 * Consumed -- published by resume-parser-service as ResumeAnalyzedEvent on topic
 * resume-parser.resume.analyzed. Field names here must match that event's JSON
 * shape exactly (overallMatchScore, not matchScore) -- Jackson binds by property
 * name, and a mismatch here silently deserializes to null rather than failing loudly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchedEvent {
    private UUID applicationId;
    private Integer overallMatchScore;
}
