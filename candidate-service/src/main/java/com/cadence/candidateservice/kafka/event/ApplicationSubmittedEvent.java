package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by Job Service (applicant count) and future Notification Service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationSubmittedEvent {
    private UUID applicationId;
    private UUID candidateId;
    private UUID jobId;
    private UUID companyId;
    private LocalDateTime occurredAt;
}
