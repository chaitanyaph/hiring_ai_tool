package com.cadence.codingassessmentservice.kafka.event;

import com.cadence.codingassessmentservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors ai-interview-service's own CandidateRecommendedEvent field-for-field -- the trigger for building the assessment_eligibility pool. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateRecommendedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private HiringRecommendation hiringRecommendation;
    private LocalDateTime occurredAt;
}
