package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** EXACT shape application-service publishes on application.offer.accepted -- also consumed by notification-service's OFFER_ACCEPTED template. Do not rename. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationOfferAcceptedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
