package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors company-service's exact shape -- its own javadoc names notification-service as the intended consumer. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamInvitationCreatedEvent {
    private UUID invitationId;
    private UUID companyId;
    private String email;
    private String firstName;
    private String role;
    private String inviteToken;
    private LocalDateTime expiryDate;
    private LocalDateTime occurredAt;
}
