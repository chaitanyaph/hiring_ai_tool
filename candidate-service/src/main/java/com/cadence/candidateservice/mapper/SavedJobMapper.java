package com.cadence.candidateservice.mapper;

import com.cadence.candidateservice.dto.response.SavedJobResponse;
import com.cadence.candidateservice.entity.SavedJob;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SavedJobMapper {
    SavedJobResponse toResponse(SavedJob savedJob);
    List<SavedJobResponse> toResponseList(List<SavedJob> savedJobs);
}
