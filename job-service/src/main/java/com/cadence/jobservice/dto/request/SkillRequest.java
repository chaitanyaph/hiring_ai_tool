package com.cadence.jobservice.dto.request;

import com.cadence.jobservice.constant.SkillType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillRequest {

    @NotBlank(message = "Skill name is required")
    private String skillName;

    @NotNull(message = "Skill type is required")
    private SkillType skillType;
}
