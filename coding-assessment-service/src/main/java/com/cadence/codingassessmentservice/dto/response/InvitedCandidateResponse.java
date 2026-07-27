package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row in the Assessment Details drawer's "Invited candidates" table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitedCandidateResponse {
    private UUID applicationId;
    private String candidateName;
    private String candidateEmail;
    private CandidateAssessmentStatus status;
    private LocalDateTime expiresAt;
}
