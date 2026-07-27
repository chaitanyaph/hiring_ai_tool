package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors resume-service's own shape (resume.resume.uploaded) -- not candidate-service's separate/different candidate.resume.uploaded event, see README §B2. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeUploadedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private String checksum;
    private LocalDateTime occurredAt;
}
