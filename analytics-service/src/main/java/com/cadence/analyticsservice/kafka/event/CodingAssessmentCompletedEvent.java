package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Smallest event in the platform -- no occurredAt, confirmed exact shape. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
}
