package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.SkillCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillResponse {
    private String skillName;
    private SkillCategory skillCategory;
}
