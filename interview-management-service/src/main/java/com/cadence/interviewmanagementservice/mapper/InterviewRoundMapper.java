package com.cadence.interviewmanagementservice.mapper;

import com.cadence.interviewmanagementservice.dto.response.InterviewRoundResponse;
import com.cadence.interviewmanagementservice.entity.InterviewRound;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InterviewRoundMapper {
    InterviewRoundResponse toResponse(InterviewRound round);
}
