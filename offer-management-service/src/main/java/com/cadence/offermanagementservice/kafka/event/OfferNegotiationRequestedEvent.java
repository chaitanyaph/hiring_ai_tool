package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferNegotiationRequestedEvent {
    private UUID offerId;
    private UUID applicationId;
    private UUID candidateId;
    private BigDecimal proposedCtc;
}
