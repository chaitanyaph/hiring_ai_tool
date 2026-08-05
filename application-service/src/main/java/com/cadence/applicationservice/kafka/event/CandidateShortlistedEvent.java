package com.cadence.applicationservice.kafka.event;

import com.cadence.applicationservice.constant.ShortlistDecision;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumed -- published by ai-interview-service's ShortlistingServiceImpl on
 * topic ai-interview.candidate.shortlisted. Field names must match that
 * side's CandidateShortlistedEvent exactly -- decision in particular used to
 * be silently dropped here (this DTO only had applicationId), which meant
 * every candidate was unconditionally advanced to AI_INTERVIEW_PENDING
 * regardless of whether the AI actually recommended REJECTED or
 * MANUAL_REVIEW. See handleCandidateShortlisted().
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateShortlistedEvent {
    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;
    private ShortlistDecision decision;
    private Integer overallMatchScore;
    private LocalDateTime occurredAt;
}
