package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published by application-service -- no separate offer-management-service exists, see README top-of-file flag. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferAcceptedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
