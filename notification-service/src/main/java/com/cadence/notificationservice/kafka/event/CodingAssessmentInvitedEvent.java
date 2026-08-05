package com.cadence.notificationservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed -- published by coding-assessment-service's CandidateAssessmentServiceImpl.inviteCandidate() on topic assessment.coding.invited, once the CandidateAssessment invite row (with its real invitedAt/expiresAt) exists. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodingAssessmentInvitedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private String assessmentName;
    private Integer durationMinutes;
    private Integer passingScorePercent;
    private String assessmentLink;
    private LocalDateTime expiresAt;
    private LocalDateTime occurredAt;
}
