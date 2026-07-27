package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Consumed. Published by Auth Service once the invited user has set a
 * password and their account is active -- carries the invite token so
 * this service can locate and close out the matching invitation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationAcceptedEvent {
    private String inviteToken;
    private LocalDateTime occurredAt;
}
