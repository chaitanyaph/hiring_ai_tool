package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitCodeRequest {
    @NotNull
    private UUID candidateAssessmentId;

    @NotNull
    private UUID questionId;

    @NotNull
    private ProgrammingLanguage language;

    @NotBlank
    private String code;
}
