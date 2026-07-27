package com.cadence.offermanagementservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** EXACT shape application-service's own ApplicationEventConsumer already actively deserializes on offer.offer.released -- do not rename. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationOfferReleasedEvent {
    private UUID applicationId;
    private UUID offerId;
}
