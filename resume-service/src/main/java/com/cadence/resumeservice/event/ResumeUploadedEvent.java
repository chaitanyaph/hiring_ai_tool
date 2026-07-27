package com.cadence.resumeservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Triggers the future Resume Parser Service to start processing this resume. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadedEvent {
    private UUID resumeId;
    private UUID candidateId;
    private String checksum;
    private LocalDateTime occurredAt;
}
