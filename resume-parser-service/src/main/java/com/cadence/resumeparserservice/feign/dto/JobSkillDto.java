package com.cadence.resumeparserservice.feign.dto;

import lombok.*;

/** Mirrors Job Service's SkillResponse -- skillType is REQUIRED or PREFERRED (Job Service has no proficiency/category concept, just this flat tag). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobSkillDto {
    private String skillName;
    private String skillType;
}
