package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/** Field shape confirmed by the seeded codingQuestions[] mockup data -- no dedicated creation screen is exported, but the data model and Module 2 of the spec are unambiguous. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {
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
    private List<StarterCodeItem> starterCodes;

    @NotEmpty @Valid
    private List<TestCaseItem> testCases;

    /** Save as draft (default) vs. immediately mark ACTIVE and usable in the assessment builder. */
    @Builder.Default
    private boolean activateNow = false;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StarterCodeItem {
        @NotNull
        private ProgrammingLanguage language;
        @NotBlank
        private String code;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestCaseItem {
        @NotNull
        private com.cadence.codingassessmentservice.constants.TestCaseVisibility visibility;
        private String inputData;
        @NotBlank
        private String expectedOutput;
        private String explanation;
        @Min(1)
        private Integer weight;
    }
}
