package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiringManagerAssignedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID hiringManagerId;
    private LocalDateTime occurredAt;
}
