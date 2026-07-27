package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row in a candidate's cross-job analysis history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateAnalysisItemResponse {
    private UUID applicationId;
    private UUID jobId;
    private ResumeMatchStatus status;
    private Integer overallMatchScore;
    private LocalDateTime analyzedAt;
}
