package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuestionRequest {
    @NotBlank
    private String title;

    @NotNull
    private Difficulty difficulty;

    @NotNull @Min(1)
    private Integer marks;

    @NotBlank
    private String description;

    private String exampleText;
    private String constraintsText;
    private String inputFormat;
    private String outputFormat;
    private String explanation;
    private List<String> tags;
    private List<String> topics;
    private List<String> hints;

    @Min(1)
    private Integer timeLimitMs;

    @Min(1)
    private Integer memoryLimitMb;

    @NotEmpty
    private List<ProgrammingLanguage> allowedLanguages;

    @Valid
    private List<CreateQuestionRequest.StarterCodeItem> starterCodes;

    @NotEmpty @Valid
    private List<CreateQuestionRequest.TestCaseItem> testCases;
}
