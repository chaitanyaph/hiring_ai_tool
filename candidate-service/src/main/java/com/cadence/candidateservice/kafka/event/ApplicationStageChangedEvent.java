package com.cadence.candidateservice.kafka.event;

import com.cadence.candidateservice.constant.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by future Notification Service to alert the candidate their application moved stage. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStageChangedEvent {
    private UUID applicationId;
    private UUID candidateId;
    private UUID jobId;
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private LocalDateTime occurredAt;
}
