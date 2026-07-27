package com.cadence.candidateservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by a future AI Resume Parsing service to extract skills/experience and score the resume. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadedEvent {
    private UUID candidateId;
    private String resumeUrl;
    private String resumeFilename;
    private LocalDateTime occurredAt;
}
