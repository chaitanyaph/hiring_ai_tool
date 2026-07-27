package com.cadence.companyservice.kafka.event;

import com.cadence.companyservice.constant.TeamRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed by Notification Service to actually send the invite email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInvitationCreatedEvent {
    private UUID invitationId;
    private UUID companyId;
    private String email;
    private String firstName;
    private TeamRole role;
    private String inviteToken;
    private LocalDateTime expiryDate;
    private LocalDateTime occurredAt;
}
