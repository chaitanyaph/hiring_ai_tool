package com.cadence.interviewmanagementservice.mapper;

import com.cadence.interviewmanagementservice.dto.response.CandidateTimelineResponse;
import com.cadence.interviewmanagementservice.entity.CandidateTimeline;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CandidateTimelineMapper {
    CandidateTimelineResponse toResponse(CandidateTimeline timeline);
}
