package com.cadence.aiinterviewservice.kafka.event;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateShortlistedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private ShortlistDecision decision;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
