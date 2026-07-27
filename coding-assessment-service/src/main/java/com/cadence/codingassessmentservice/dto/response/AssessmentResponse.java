package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.AssessmentType;
import com.cadence.codingassessmentservice.constants.Difficulty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Backs the wizard's own review step response and any GET by id -- jobTitle is enriched via Feign, not stored. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentResponse {
    private UUID id;
    private UUID companyId;
    private UUID jobId;
    private String jobTitle;
    private String name;
    private AssessmentType type;
    private AssessmentStatus status;
    private Difficulty difficulty;
    private int durationMinutes;
    private int questionCount;
    private int passingScorePercent;
    private int totalMarks;
    private String compilerVersion;
    private List<String> allowedLanguages;
    private boolean negativeMarking;
    private boolean antiCheatMonitoring;
    private boolean plagiarismDetection;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String candidateInstructions;
    private LocalDateTime createdAt;
}
