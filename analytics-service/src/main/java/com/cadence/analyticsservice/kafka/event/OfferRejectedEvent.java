package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferRejectedEvent {
    private UUID offerId;
    private UUID applicationId;
    private UUID candidateId;
    private String reason;
}
