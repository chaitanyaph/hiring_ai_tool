package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** The full aggregate backing the Resume Analysis drawer -- one job/candidate match with every AI-generated sub-section attached. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeMatchResponse {
    private UUID id;
    private UUID applicationId;
    private UUID jobId;
    private UUID departmentId;
    private UUID candidateId;
    private UUID resumeId;
    private String fullName;
    private String email;
    private String professionalSummary;
    private ResumeMatchStatus status;
    private int attemptCount;
    private AiProvider providerUsed;

    private Integer overallMatchScore;
    private Integer technicalSkillScore;
    private Integer programmingLanguageScore;
    private Integer frameworkScore;
    private Integer databaseScore;
    private Integer cloudScore;
    private Integer devopsScore;
    private ExperienceMatchLabel experienceMatchLabel;
    private Integer projectMatchScore;
    private EducationMatchLabel educationMatchLabel;
    private CertificationMatchLabel certificationMatchLabel;
    private Integer communicationPrediction;
    private Integer problemSolvingPrediction;
    private Integer learningAbilityPrediction;

    private List<MatchedSkillResponse> matchedSkills;
    private List<MissingSkillResponse> missingSkills;
    private List<StrengthWeaknessResponse> strengths;
    private List<StrengthWeaknessResponse> weaknesses;
    private AiRecommendationResponse aiRecommendation;

    private String failureReason;
    private LocalDateTime analyzedAt;
}
