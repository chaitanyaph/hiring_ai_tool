package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed. Published by Auth Service after it provisions a login
 * account for an accepted invitation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private UUID userId;
    private String email;
    private UUID companyId;
    private LocalDateTime occurredAt;
}
