package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.AiProvider;
import com.cadence.resumeparserservice.constants.ParsingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The drawer's main aggregate view -- resume summary + every extracted
 * section in one call, since that's exactly how the Figma renders it
 * (one drawer, one fetch). Also served as-is to the internal endpoint
 * for the not-yet-built Matching/Shortlisting services.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedResumeResponse {
    private UUID resumeId;
    private UUID candidateId;
    private ParsingStatus status;
    private int attemptCount;
    private AiProvider providerUsed;

    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String professionalSummary;
    private String currentCompany;
    private String currentDesignation;
    private BigDecimal totalExperienceYears;
    private String noticePeriod;
    private String expectedSalary;
    private String failureReason;
    private LocalDateTime parsedAt;

    private List<SkillResponse> skills;
    private List<ExperienceResponse> experience;
    private List<EducationResponse> education;
    private List<ProjectResponse> projects;
    private List<CertificationResponse> certifications;
    private List<AchievementResponse> achievements;
    private List<LanguageResponse> languages;
}
