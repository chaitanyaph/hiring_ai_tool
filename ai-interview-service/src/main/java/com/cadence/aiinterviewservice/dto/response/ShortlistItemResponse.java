package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backs the Shortlisted/Rejected/Manual-review tabs and the Ranking
 * ("Top Candidates") table -- one shape for all four, since they show
 * the same columns filtered/ordered differently. fullName/email/
 * jobTitle come from Application Service's own candidateNameSnapshot/
 * candidateEmailSnapshot/jobTitleSnapshot (batch-fetched once per job
 * by the query service), not a second per-row Feign call.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortlistItemResponse {
    private UUID applicationId;
    private UUID candidateId;
    private String fullName;
    private String email;
    private UUID jobId;
    private String jobTitle;
    private Integer overallMatchScore;
    private ShortlistDecision decision;
    private String reason;
    private UUID assignedRecruiterId;
    private LocalDateTime decidedAt;
}
