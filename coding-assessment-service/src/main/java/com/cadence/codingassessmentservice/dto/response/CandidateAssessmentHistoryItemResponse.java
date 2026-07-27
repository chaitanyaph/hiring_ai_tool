package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs #csec-coding-history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateAssessmentHistoryItemResponse {
    private UUID candidateAssessmentId;
    private UUID assessmentId;
    private String assessmentName;
    private String companyName;
    private CandidateAssessmentStatus status;
    private Integer score;
    private LocalDateTime relevantDate; // completedAt, or expiresAt while pending, or expiry timestamp if expired
}
