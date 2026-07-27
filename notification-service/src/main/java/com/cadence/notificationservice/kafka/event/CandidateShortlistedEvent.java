package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** decision is a String mirror of ai-interview-service's ShortlistDecision enum (e.g. SHORTLISTED/REJECTED) -- deserialized loosely since that enum's exact values live in a sibling service. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateShortlistedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String decision;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
