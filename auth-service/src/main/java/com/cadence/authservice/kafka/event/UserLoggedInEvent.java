package com.cadence.authservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLoggedInEvent {
    private UUID userId;
    private String email;
    private String ipAddress;
    private String device;
    private LocalDateTime occurredAt;
}
