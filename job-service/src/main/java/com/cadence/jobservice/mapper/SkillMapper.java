package com.cadence.jobservice.mapper;

import com.cadence.jobservice.dto.response.SkillResponse;
import com.cadence.jobservice.entity.JobSkill;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SkillMapper {
    SkillResponse toResponse(JobSkill skill);
}
