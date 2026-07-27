package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Consumed -- published by the (future) Coding Assessment Service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingAssessmentCompletedEvent {
    private UUID applicationId;
    private Integer score;
}
