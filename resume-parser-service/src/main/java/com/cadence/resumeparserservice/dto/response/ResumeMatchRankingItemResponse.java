package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.ExperienceMatchLabel;
import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row in the per-job candidate ranking table. fullName/email come
 * from this service's own parsed_resume (joined by parsedResumeId) --
 * same "only populated once actually known" precedent as
 * ParsingQueueItemResponse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchRankingItemResponse {
    private UUID applicationId;
    private UUID candidateId;
    private UUID resumeId;
    private String fullName;
    private String email;
    private ResumeMatchStatus status;
    private Integer overallMatchScore;
    private ExperienceMatchLabel experienceMatchLabel;
    private LocalDateTime analyzedAt;
}
