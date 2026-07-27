package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** EXACT shape application-service's ApplicationEventConsumer already actively deserializes -- do not add or rename fields without checking that side first. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
}
