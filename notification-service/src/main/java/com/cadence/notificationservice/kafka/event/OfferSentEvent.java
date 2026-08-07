package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Consumed -- published by offer-management-service on topic offer-management.offer.sent, the moment an offer is actually sent to the candidate. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferSentEvent {
    private UUID offerId;
    private UUID applicationId;
    private UUID jobId;
    private UUID companyId;
    private UUID candidateId;
    private String candidateEmail;
}
