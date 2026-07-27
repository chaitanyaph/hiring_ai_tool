package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Backs the #ca-intro screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateAssessmentIntroResponse {
    private UUID candidateAssessmentId;
    private String jobTitle;
    private String companyName;
    private int durationMinutes;
    private int questionCount;
    private List<String> allowedLanguages;
    private CandidateAssessmentStatus status;
    private String candidateInstructions;
}
