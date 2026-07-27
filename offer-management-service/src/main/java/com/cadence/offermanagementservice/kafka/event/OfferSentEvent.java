package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferSentEvent {
    private UUID offerId;
    private UUID applicationId;
    private UUID candidateId;
    private String candidateEmail;
}
