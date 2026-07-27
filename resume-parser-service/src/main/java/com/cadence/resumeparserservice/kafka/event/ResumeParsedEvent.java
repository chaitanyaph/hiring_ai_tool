package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published on successful parse -- forward-looking scaffolding for the not-yet-built Matching/Shortlisting services. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeParsedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private UUID parsedResumeId;
    private LocalDateTime occurredAt;
}
