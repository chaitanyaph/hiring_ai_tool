package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUnsavedEvent {
    private UUID candidateId;
    private UUID jobId;
    private LocalDateTime occurredAt;
}
