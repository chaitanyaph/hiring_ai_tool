package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileCreatedEvent {
    private UUID candidateId;
    private String fullName;
    private String email;
    private LocalDateTime occurredAt;
}
