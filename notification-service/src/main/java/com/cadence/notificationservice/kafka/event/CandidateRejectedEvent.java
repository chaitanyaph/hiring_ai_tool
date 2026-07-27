package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateRejectedEvent {
    private UUID applicationId;
    private UUID candidateId;
    private String reason;
}
