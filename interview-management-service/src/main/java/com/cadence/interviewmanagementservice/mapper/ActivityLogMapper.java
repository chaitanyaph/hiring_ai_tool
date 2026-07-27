package com.cadence.interviewmanagementservice.mapper;

import com.cadence.interviewmanagementservice.dto.response.ActivityLogResponse;
import com.cadence.interviewmanagementservice.entity.InterviewActivityLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActivityLogMapper {
    ActivityLogResponse toResponse(InterviewActivityLog log);
}
