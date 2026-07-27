package com.cadence.interviewmanagementservice.mapper;

import com.cadence.interviewmanagementservice.dto.response.InterviewDetailResponse;
import com.cadence.interviewmanagementservice.dto.response.InterviewListItemResponse;
import com.cadence.interviewmanagementservice.entity.Interview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** candidateName/jobTitle/companyName/panelists are enriched by the service layer via Feign + interview_panelist lookups, not stored on the entity. */
@Mapper(componentModel = "spring")
public interface InterviewMapper {

    @Mapping(target = "candidateName", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "panelists", ignore = true)
    @Mapping(target = "feedbackSubmitted", ignore = true)
    InterviewListItemResponse toListItemResponse(Interview interview);

    @Mapping(target = "candidateName", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "panelists", ignore = true)
    @Mapping(target = "feedbackSubmittable", ignore = true)
    InterviewDetailResponse toDetailResponse(Interview interview);
}
