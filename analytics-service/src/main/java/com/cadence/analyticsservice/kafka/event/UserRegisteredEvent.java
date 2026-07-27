package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegisteredEvent {
    private UUID userId;
    private String fullName;
    private String email;
    private String userType;
    private UUID companyId;
    private String verificationLink;
    private LocalDateTime occurredAt;
}
