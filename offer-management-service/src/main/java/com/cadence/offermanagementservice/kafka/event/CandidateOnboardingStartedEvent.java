package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateOnboardingStartedEvent {
    private UUID offerId;
    private UUID applicationId;
    private UUID candidateId;
    private LocalDate startDate;
}
