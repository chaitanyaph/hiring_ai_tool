package com.cadence.aiinterviewservice.mapper;

import com.cadence.aiinterviewservice.dto.response.ShortlistItemResponse;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** fullName/email/jobTitle come from Application Service's snapshot fields, batch-fetched once per job by the query service -- not stored on candidate_shortlist itself. */
@Mapper(componentModel = "spring")
public interface ShortlistMapper {

    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "jobTitle", ignore = true)
    ShortlistItemResponse toResponse(CandidateShortlist shortlist);
}
