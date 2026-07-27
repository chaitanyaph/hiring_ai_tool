package com.cadence.resumeservice.mapper;

import com.cadence.resumeservice.dto.response.ResumeObjectDetailsResponse;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.entity.Resume;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

    ResumeResponse toResponse(Resume resume);

    List<ResumeResponse> toResponseList(List<Resume> resumes);

    @org.mapstruct.Mapping(target = "resumeId", source = "id")
    ResumeObjectDetailsResponse toObjectDetailsResponse(Resume resume);
}
