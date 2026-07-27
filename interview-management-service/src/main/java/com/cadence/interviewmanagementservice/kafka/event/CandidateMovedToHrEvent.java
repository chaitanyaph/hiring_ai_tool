package com.cadence.interviewmanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateMovedToHrEvent {
    private UUID applicationId;
    private UUID candidateId;
}
