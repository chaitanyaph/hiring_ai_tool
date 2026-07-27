package com.cadence.interviewmanagementservice.kafka.event;

import com.cadence.interviewmanagementservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors ai-interview-service's own CandidateRecommendedEvent field-for-field -- consumed to populate the AI_INTERVIEW candidate_timeline stage. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateRecommendedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private HiringRecommendation hiringRecommendation;
    private LocalDateTime occurredAt;
}
