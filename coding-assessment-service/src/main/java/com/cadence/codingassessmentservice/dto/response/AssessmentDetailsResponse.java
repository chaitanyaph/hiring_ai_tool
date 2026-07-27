package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.util.List;

/** Backs openAssessmentDetailsDrawer -- the assessment fields plus a derived "rules" bullet list and the invited-candidates table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentDetailsResponse {
    private AssessmentResponse assessment;
    private List<String> rules;
    private List<InvitedCandidateResponse> invitedCandidates;
}
