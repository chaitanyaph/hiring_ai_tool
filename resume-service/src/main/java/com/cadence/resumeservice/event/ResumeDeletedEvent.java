package com.cadence.resumeservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDeletedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private LocalDateTime occurredAt;
}
