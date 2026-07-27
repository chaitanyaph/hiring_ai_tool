package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

/** Mirrors Resume Parser Service's ResumeMatchResponse -- only the fields this service needs for shortlisting and question-generation context. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMatchDto {
    private UUID id;
    private UUID applicationId;
    private UUID jobId;
    private UUID departmentId;
    private UUID candidateId;
    private String fullName;
    private String email;
    private String professionalSummary;
    private String status;
    private Integer overallMatchScore;
    private Integer technicalSkillScore;
    private String experienceMatchLabel;
    private String educationMatchLabel;
    private List<MatchedSkillDto> matchedSkills;
    private List<MissingSkillDto> missingSkills;
}
