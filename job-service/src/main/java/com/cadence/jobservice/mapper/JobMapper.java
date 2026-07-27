package com.cadence.jobservice.mapper;

import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobSummaryResponse;
import com.cadence.jobservice.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "departmentName", ignore = true)
    @Mapping(target = "applicantsCount", ignore = true)
    JobSummaryResponse toSummary(Job job);

    @Mapping(target = "descriptionHtml", ignore = true)
    @Mapping(target = "requirements", ignore = true)
    @Mapping(target = "pipelineStages", ignore = true)
    JobDetailResponse toDetail(Job job);
}
