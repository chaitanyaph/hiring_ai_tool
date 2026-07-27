package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterAssignedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID recruiterId;
    private LocalDateTime occurredAt;
}
