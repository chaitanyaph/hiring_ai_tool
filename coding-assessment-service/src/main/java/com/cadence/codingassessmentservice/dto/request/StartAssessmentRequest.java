package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import lombok.*;

/** The intro screen's "Preferred language" select -- optional; if omitted, the candidate picks per-question in the IDE. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartAssessmentRequest {
    private ProgrammingLanguage preferredLanguage;
}
