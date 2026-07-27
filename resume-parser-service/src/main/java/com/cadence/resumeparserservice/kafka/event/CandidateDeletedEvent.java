package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors Candidate Service's own CandidateDeletedEvent field-for-field. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateDeletedEvent {
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
