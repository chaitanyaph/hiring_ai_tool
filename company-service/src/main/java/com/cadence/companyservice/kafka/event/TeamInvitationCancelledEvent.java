package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInvitationCancelledEvent {
    private UUID invitationId;
    private UUID companyId;
    private String email;
    private LocalDateTime occurredAt;
}
