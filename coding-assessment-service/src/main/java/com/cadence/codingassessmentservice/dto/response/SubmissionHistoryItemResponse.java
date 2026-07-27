package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs openSubmissionHistoryModal -- one row per attempt, all attempts kept. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionHistoryItemResponse {
    private UUID submissionId;
    private int questionOrder;
    private LocalDateTime submittedAt;
    private String language;
    private SubmissionStatus status;
    private Integer runtimeMs;
    private Integer memoryKb;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private Integer score;
}
