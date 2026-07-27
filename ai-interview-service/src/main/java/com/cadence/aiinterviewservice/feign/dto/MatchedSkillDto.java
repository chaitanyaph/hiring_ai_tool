package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchedSkillDto {
    private String skillName;
    private String skillCategory;
}
