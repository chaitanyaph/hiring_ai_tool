package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The funnel backbone -- fromStatus/toStatus mirror application-
 * service's own ApplicationStatus enum values (deserialized loosely
 * as String since that enum's exact values live in a sibling service).
 * One consumer method on this event drives most of the funnel/KPI
 * counters.
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
