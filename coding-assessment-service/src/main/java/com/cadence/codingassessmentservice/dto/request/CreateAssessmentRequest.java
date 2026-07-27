package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.AssessmentType;
import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Mirrors the 4-step creation wizard field-for-field (Basic info + Configuration + Schedule & rules), submitted once at wizard-finish. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAssessmentRequest {
    @NotBlank
    private String name;

    @NotNull
    private UUID jobId;

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

    @Builder.Default
    private boolean negativeMarking = false;

    @Builder.Default
    private boolean antiCheatMonitoring = true;

    @Builder.Default
    private boolean plagiarismDetection = true;

    private LocalDate startDate;
    private LocalDate expiryDate;
    private String candidateInstructions;

    /** Publish immediately (wizard step 4 "Publish now") vs. save as draft. */
    @Builder.Default
    private boolean publishNow = false;

    /** No question-picker exists in the wizard -- questions are wired in separately via PUT /assessments/{id}/questions, but may also be supplied here for convenience. */
    private List<UUID> questionIds;
}
