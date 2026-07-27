package com.cadence.applicationservice.mapper;

import com.cadence.applicationservice.dto.response.ApplicationResponse;
import com.cadence.applicationservice.dto.response.NoteResponse;
import com.cadence.applicationservice.dto.response.StageHistoryResponse;
import com.cadence.applicationservice.dto.response.StatusHistoryResponse;
import com.cadence.applicationservice.entity.Application;
import com.cadence.applicationservice.entity.ApplicationNote;
import com.cadence.applicationservice.entity.ApplicationStageHistory;
import com.cadence.applicationservice.entity.ApplicationStatusHistory;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "notes", ignore = true)
    ApplicationResponse toResponse(Application application);

    List<ApplicationResponse> toResponseList(List<Application> applications);

    NoteResponse toResponse(ApplicationNote note);

    List<NoteResponse> toNoteResponseList(List<ApplicationNote> notes);

    StatusHistoryResponse toResponse(ApplicationStatusHistory history);

    List<StatusHistoryResponse> toStatusHistoryResponseList(List<ApplicationStatusHistory> history);

    StageHistoryResponse toResponse(ApplicationStageHistory history);

    List<StageHistoryResponse> toStageHistoryResponseList(List<ApplicationStageHistory> history);
}
