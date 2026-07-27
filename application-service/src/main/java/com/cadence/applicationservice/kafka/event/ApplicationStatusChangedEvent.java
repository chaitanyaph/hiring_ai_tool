package com.cadence.applicationservice.kafka.event;

import com.cadence.applicationservice.constant.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by the future Notification Service to email/notify the candidate on every stage change. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusChangedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private LocalDateTime occurredAt;
}
