package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Backs Tab 1's "Assessments" table. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentListItemResponse {
    private UUID id;
    private String name;
    private String jobTitle;
    private List<String> allowedLanguages;
    private int durationMinutes;
    private int questionCount;
    private AssessmentStatus status;
    private long invitedCount;
}
