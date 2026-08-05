package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed -- published by ai-interview-service's InterviewSessionServiceImpl.inviteCandidate() on topic ai-interview.interview.invited, once the actual InterviewSession row (with its real invitedAt/expiresAt) exists -- not the earlier CandidateShortlistedEvent, which fires before invitation. */
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
