package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed -- published by application-service on topic application.status.changed for
 * EVERY status transition, manual or automatic (see ApplicationServiceImpl.transitionStatus()).
 * fromStatus/toStatus are String mirrors of application-service's own ApplicationStatus enum --
 * deserialized loosely since that enum's full 21-value set lives in a sibling service.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationStatusChangedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime occurredAt;
}
