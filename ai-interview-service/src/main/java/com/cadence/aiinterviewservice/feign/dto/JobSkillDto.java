package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

/** Mirrors Job Service's SkillResponse -- skillType is REQUIRED or PREFERRED. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillDto {
    private String skillName;
    private String skillType;
}
