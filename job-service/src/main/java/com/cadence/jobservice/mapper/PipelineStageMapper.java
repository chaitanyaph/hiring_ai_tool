package com.cadence.jobservice.mapper;

import com.cadence.jobservice.dto.response.PipelineStageResponse;
import com.cadence.jobservice.entity.JobPipelineStage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PipelineStageMapper {
    PipelineStageResponse toResponse(JobPipelineStage stage);
}
