package com.cadence.codingassessmentservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Published the moment a candidate's coding-assessment invite (auto-eligibility or manual) actually creates/refreshes the CandidateAssessment row -- carries the real link and validity window. */
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
