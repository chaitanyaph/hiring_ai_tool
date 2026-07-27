package com.cadence.resumeservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed -- published by Candidate Service when a candidate account is deleted (no such flow exists there yet; this consumer is forward-looking, same as the platform's other cross-service event scaffolding). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDeletedEvent {
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
