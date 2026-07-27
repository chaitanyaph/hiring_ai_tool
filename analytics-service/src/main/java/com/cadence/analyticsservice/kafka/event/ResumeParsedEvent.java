package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeParsedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private UUID parsedResumeId;
    private LocalDateTime occurredAt;
}
