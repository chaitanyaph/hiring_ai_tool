package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.AssessmentType;
import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAssessmentRequest {
    @NotBlank
    private String name;

    @NotNull
    private AssessmentType type;

    @NotEmpty
    private List<ProgrammingLanguage> allowedLanguages;

    @NotNull
    private Difficulty difficulty;

    @NotNull @Min(1)
    private Integer durationMinutes;

    @NotNull @Min(1)
    private Integer questionCount;

    @NotNull @Min(0) @Max(100)
    private Integer passingScorePercent;

    @NotNull @Min(1)
    private Integer totalMarks;

    private String compilerVersion;
    private boolean negativeMarking;
    private boolean antiCheatMonitoring;
    private boolean plagiarismDetection;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String candidateInstructions;
}
