package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Consumed -- published by the (future) Offer Service once an offer letter has been generated and sent. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferReleasedEvent {
    private UUID applicationId;
    private UUID offerId;
}
