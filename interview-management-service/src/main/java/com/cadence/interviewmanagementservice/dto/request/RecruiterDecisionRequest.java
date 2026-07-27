package com.cadence.interviewmanagementservice.dto.request;

import com.cadence.interviewmanagementservice.constants.DecisionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** Module 6: Move to HR / Schedule next round / Select / Reject / Hold / Request another interview. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterDecisionRequest {

    @NotNull
    private DecisionType decisionType;

    private String notes;
}
