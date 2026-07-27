package com.cadence.companyservice.mapper;

import com.cadence.companyservice.dto.response.TeamInvitationResponse;
import com.cadence.companyservice.entity.TeamInvitation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeamInvitationMapper {
    TeamInvitationResponse toResponse(TeamInvitation invitation);
}
