package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs the candidate's #ca-result screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentResultResponse {
    private UUID candidateAssessmentId;
    private Integer scorePercent;
    private Boolean passed;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private Integer timeUsedMinutes;
    private boolean autoSubmitted;
    private LocalDateTime submittedAt;
}
