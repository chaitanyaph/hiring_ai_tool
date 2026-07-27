package com.cadence.candidateservice.mapper;

import com.cadence.candidateservice.dto.response.ApplicationResponse;
import com.cadence.candidateservice.dto.response.ApplicationStatusHistoryResponse;
import com.cadence.candidateservice.entity.Application;
import com.cadence.candidateservice.entity.ApplicationStatusHistory;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "history", ignore = true)
    ApplicationResponse toResponse(Application application);

    List<ApplicationResponse> toResponseList(List<Application> applications);

    ApplicationStatusHistoryResponse toResponse(ApplicationStatusHistory history);

    List<ApplicationStatusHistoryResponse> toHistoryResponseList(List<ApplicationStatusHistory> history);
}
