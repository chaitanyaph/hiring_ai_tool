package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeUpdatedEvent {
    private UUID officeId;
    private UUID companyId;
    private String officeName;
    private LocalDateTime occurredAt;
}
