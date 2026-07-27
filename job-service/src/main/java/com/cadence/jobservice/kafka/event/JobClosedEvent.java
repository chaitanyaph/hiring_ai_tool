package com.cadence.jobservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobClosedEvent {
    private UUID jobId;
    private UUID companyId;
    private LocalDateTime occurredAt;
}
