package com.cadence.candidateservice.mapper;

import com.cadence.candidateservice.dto.response.*;
import com.cadence.candidateservice.entity.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

    EducationResponse toResponse(CandidateEducation entity);
    List<EducationResponse> toEducationResponseList(List<CandidateEducation> entities);

    ExperienceResponse toResponse(CandidateExperience entity);
    List<ExperienceResponse> toExperienceResponseList(List<CandidateExperience> entities);

    ProjectResponse toResponse(CandidateProject entity);
    List<ProjectResponse> toProjectResponseList(List<CandidateProject> entities);

    CertificationResponse toResponse(CandidateCertification entity);
    List<CertificationResponse> toCertificationResponseList(List<CandidateCertification> entities);

    JobPreferencesResponse toResponse(CandidateJobPreference entity);

    PortfolioResponse toResponse(CandidatePortfolioLink entity);
}
