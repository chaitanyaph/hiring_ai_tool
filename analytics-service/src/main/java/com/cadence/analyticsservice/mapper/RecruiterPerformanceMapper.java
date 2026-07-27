package com.cadence.analyticsservice.mapper;

import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.entity.RecruiterPerformanceSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** recruiterName is enriched by the service layer via a Feign lookup, not stored on the snapshot (no dedicated recruiter/user lookup client exists on this platform yet -- best-effort, degrades to null). */
@Mapper(componentModel = "spring")
public interface RecruiterPerformanceMapper {

    @Mapping(target = "recruiterName", ignore = true)
    RecruiterPerformanceResponse toResponse(RecruiterPerformanceSnapshot snapshot);
}
