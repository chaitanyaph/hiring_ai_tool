package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row in the Analysis dashboard's "Completed interviews" table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCompletedItemResponse {
    private UUID applicationId;
    private UUID candidateId;
    private String fullName;
    private UUID jobId;
    private String jobTitle;
    private Integer overallScore;
    private HiringRecommendation hiringRecommendation;
    private LocalDateTime completedAt;
}
