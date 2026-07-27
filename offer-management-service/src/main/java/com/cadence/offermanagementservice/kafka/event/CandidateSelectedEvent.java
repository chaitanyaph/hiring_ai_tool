package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** EXACT shape interview-management-service publishes. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateSelectedEvent {
    private UUID applicationId;
    private UUID candidateId;
}
