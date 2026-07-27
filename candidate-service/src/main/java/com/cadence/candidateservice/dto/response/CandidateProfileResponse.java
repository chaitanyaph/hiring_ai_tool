package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Backs both the wizard's review state and the read-only "My Profile" screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfileResponse {
    private UUID id;
    private String fullName;
    private String headline;
    private String email;
    private String phone;
    private String location;
    private String currentCompany;
    private Integer noticePeriodDays;
    private String profilePhotoUrl;
    private String resumeUrl;
    private String resumeFilename;
    private LocalDateTime resumeParsedAt;
    private Integer aiResumeScore;
    private Integer profileCompletionPercent;
    private String status;

    private List<EducationResponse> education;
    private List<ExperienceResponse> experience;
    private List<String> skills;
    private List<ProjectResponse> projects;
    private List<CertificationResponse> certifications;
    private List<String> languages;
    private JobPreferencesResponse jobPreferences;
    private PortfolioResponse portfolio;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
