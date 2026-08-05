package com.cadence.aiinterviewservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published the moment an interview invite (auto or manual) actually creates/refreshes the InterviewSession row -- carries the real link and validity window, unlike CandidateShortlistedEvent which fires before any session exists. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInterviewInvitedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String interviewLink;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime occurredAt;
}
