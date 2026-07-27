package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

import java.util.List;

/** Mirrors Job Service's JobRequirementsResponse -- only the fields this service needs for question generation. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobRequirementsDto {
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private List<JobSkillDto> skills;
    private String education;
    private String certifications;
}
