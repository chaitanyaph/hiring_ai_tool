package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** EXACT 2-field shape confirmed real and working end-to-end elsewhere on the platform. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
}
