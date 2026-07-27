package com.cadence.jobservice.dto.response;

import com.cadence.jobservice.constant.SkillType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillResponse {
    private UUID id;
    private String skillName;
    private SkillType skillType;
}
