package com.cadence.jobservice.mapper;

import com.cadence.jobservice.dto.response.JobTemplateResponse;
import com.cadence.jobservice.entity.JobTemplate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface JobTemplateMapper {
    JobTemplateResponse toResponse(JobTemplate template);
}
