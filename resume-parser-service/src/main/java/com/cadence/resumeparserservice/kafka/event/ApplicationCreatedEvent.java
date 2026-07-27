package com.cadence.resumeparserservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mirrors Application Service's own ApplicationCreatedEvent field-for-
 * field -- this is the actual trigger for match analysis (not
 * ResumeUploaded, which has no job context). Note: resumeId may be
 * null here today -- see the README's "Architecture Decisions" for
 * why, and how AWAITING_RESUME handles it.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationCreatedEvent {
    private UUID applicationId;
    private UUID companyId;
    private UUID jobId;
    private UUID candidateId;
    private UUID resumeId;
    private LocalDateTime occurredAt;
}
