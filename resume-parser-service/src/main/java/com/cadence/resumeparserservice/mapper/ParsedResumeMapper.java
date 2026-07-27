package com.cadence.resumeparserservice.mapper;

import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.entity.*;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * The full aggregate (ParsedResumeResponse.skills/experience/...) is
 * assembled by the service layer, not here -- ParsedResume has no JPA
 * object-graph relationship to its child rows (plain UUID FK columns,
 * same style as Candidate Service's equivalent tables), so there's
 * nothing for MapStruct to walk from the entity alone.
 */
@Mapper(componentModel = "spring")
public interface ParsedResumeMapper {

    ParsedResumeResponse toResponse(ParsedResume parsedResume);

    ParsingStatusResponse toStatusResponse(ParsedResume parsedResume);

    @org.mapstruct.Mapping(target = "resumeId", source = "resumeId")
    @org.mapstruct.Mapping(target = "submittedAt", source = "createdAt")
    @org.mapstruct.Mapping(target = "progressPercent", ignore = true)
    ParsingQueueItemResponse toQueueItemResponse(ParsedResume parsedResume);

    SkillResponse toResponse(CandidateSkill skill);
    List<SkillResponse> toSkillResponseList(List<CandidateSkill> skills);

    ExperienceResponse toResponse(CandidateExperience experience);
    List<ExperienceResponse> toExperienceResponseList(List<CandidateExperience> experience);

    EducationResponse toResponse(CandidateEducation education);
    List<EducationResponse> toEducationResponseList(List<CandidateEducation> education);

    ProjectResponse toResponse(CandidateProject project);
    List<ProjectResponse> toProjectResponseList(List<CandidateProject> projects);

    CertificationResponse toResponse(CandidateCertification certification);
    List<CertificationResponse> toCertificationResponseList(List<CandidateCertification> certifications);

    AchievementResponse toResponse(CandidateAchievement achievement);
    List<AchievementResponse> toAchievementResponseList(List<CandidateAchievement> achievements);

    LanguageResponse toResponse(CandidateLanguage language);
    List<LanguageResponse> toLanguageResponseList(List<CandidateLanguage> languages);

    ParserLogResponse toResponse(ParserLog log);
    List<ParserLogResponse> toLogResponseList(List<ParserLog> logs);
}
