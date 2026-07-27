package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyCreatedEvent {
    private UUID companyId;
    private String companyName;
    private String companySlug;
    private LocalDateTime occurredAt;
}
