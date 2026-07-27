package com.cadence.authservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountLockedEvent {
    private UUID userId;
    private String email;
    private String reason;
    private LocalDateTime lockedUntil;
    private LocalDateTime occurredAt;
}
