package com.cadence.aiinterviewservice.kafka.event;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Published on successful evaluation. Carries hiringRecommendation so
 * application-service can gate AI_INTERVIEW_COMPLETED's next transition
 * on it (PROCEED -> CODING_ASSESSMENT_PENDING, REJECT -> REJECTED, HOLD ->
 * stays put for explicit recruiter review) instead of blindly advancing
 * every candidate regardless of how the interview actually went.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewEvaluatedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private UUID sessionId;
    private Integer overallScore;
    private HiringRecommendation hiringRecommendation;
    private LocalDateTime occurredAt;
}
