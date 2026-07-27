package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyUpdatedEvent {
    private UUID companyId;
    private String companyName;
    private LocalDateTime occurredAt;
}
