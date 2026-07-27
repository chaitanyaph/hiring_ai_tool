package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateShortlistedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String decision;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
