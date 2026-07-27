package com.cadence.codingassessmentservice.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinishAssessmentRequest {
    /** true when the frontend's own timer expired and auto-submitted on the candidate's behalf. */
    @Builder.Default
    private boolean autoSubmitted = false;
}
