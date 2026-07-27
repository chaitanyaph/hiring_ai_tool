package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferRejectedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
