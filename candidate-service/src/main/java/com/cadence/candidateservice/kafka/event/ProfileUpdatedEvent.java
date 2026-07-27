package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdatedEvent {
    private UUID candidateId;
    private Integer profileCompletionPercent;
    private LocalDateTime occurredAt;
}
