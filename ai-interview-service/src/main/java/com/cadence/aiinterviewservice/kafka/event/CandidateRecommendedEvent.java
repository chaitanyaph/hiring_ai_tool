package com.cadence.aiinterviewservice.kafka.event;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateRecommendedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private HiringRecommendation hiringRecommendation;
    private LocalDateTime occurredAt;
}
