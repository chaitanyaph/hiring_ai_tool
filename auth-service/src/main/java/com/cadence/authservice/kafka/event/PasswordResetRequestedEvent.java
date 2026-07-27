package com.cadence.authservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetRequestedEvent {
    private UUID userId;
    private String email;
    private String resetLink;
    private LocalDateTime occurredAt;
}
