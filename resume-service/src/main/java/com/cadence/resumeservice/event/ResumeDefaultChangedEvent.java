package com.cadence.resumeservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDefaultChangedEvent {
    private UUID candidateId;
    private UUID newDefaultResumeId;
    private UUID previousDefaultResumeId;
    private LocalDateTime occurredAt;
}
