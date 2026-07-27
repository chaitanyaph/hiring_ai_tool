package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by the future Offer Service / Notification Service once a candidate accepts. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferAcceptedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
