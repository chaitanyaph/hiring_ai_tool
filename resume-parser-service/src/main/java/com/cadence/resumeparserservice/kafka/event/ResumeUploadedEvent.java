package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors Resume Service's own ResumeUploadedEvent field-for-field -- this is the event that triggers the whole parsing pipeline. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumeUploadedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private String checksum;
    private LocalDateTime occurredAt;
}
