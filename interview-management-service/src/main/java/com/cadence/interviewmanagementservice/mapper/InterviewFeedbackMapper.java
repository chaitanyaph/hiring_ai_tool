package com.cadence.interviewmanagementservice.mapper;

import com.cadence.interviewmanagementservice.dto.response.InterviewFeedbackResponse;
import com.cadence.interviewmanagementservice.entity.InterviewFeedback;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterviewFeedbackMapper {

    @Mapping(target = "interviewerName", ignore = true)
    InterviewFeedbackResponse toResponse(InterviewFeedback feedback);
}
