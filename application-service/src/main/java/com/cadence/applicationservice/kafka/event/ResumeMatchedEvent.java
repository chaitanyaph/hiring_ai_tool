package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Consumed -- published by the (future) Resume Matching Service with the computed match score (0-100). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchedEvent {
    private UUID applicationId;
    private Integer matchScore;
}
