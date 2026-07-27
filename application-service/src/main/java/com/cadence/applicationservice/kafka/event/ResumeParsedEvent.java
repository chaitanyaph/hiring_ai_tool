package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed -- published by the (future) Resume Parser Service once it finishes parsing a candidate's resume. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeParsedEvent {
    private UUID applicationId;
    private UUID resumeId;
    private LocalDateTime parsedAt;
}
