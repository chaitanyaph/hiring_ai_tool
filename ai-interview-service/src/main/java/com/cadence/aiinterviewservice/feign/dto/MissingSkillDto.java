package com.cadence.aiinterviewservice.feign.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MissingSkillDto {
    private String skillName;
    private String skillCategory;
    private boolean required;
}
