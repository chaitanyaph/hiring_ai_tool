package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs Tab 2's coding queue table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingQueueItemResponse {
    private UUID applicationId;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private CandidateAssessmentStatus status;
    private Integer scorePercent;
    private Boolean passed;
    private LocalDateTime dueOrCompletedAt;
}
