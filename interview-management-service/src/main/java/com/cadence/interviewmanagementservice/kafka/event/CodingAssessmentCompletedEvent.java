package com.cadence.interviewmanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** EXACT shape coding-assessment-service publishes -- consumed to populate the CODING_ASSESSMENT candidate_timeline stage. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
}
