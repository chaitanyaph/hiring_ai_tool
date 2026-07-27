package com.cadence.resumeparserservice.mapper;

import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * fullName/email/professionalSummary on ResumeMatchResponse and
 * ResumeMatchRankingItemResponse come from this service's own
 * parsed_resume table (joined by parsedResumeId), not from ResumeMatch
 * itself -- the service layer sets those fields after calling these
 * mappers, same split ParsedResumeQueryServiceImpl already uses.
 */
@Mapper(componentModel = "spring")
public interface ResumeMatchMapper {

    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "professionalSummary", ignore = true)
    @Mapping(target = "matchedSkills", ignore = true)
    @Mapping(target = "missingSkills", ignore = true)
    @Mapping(target = "strengths", ignore = true)
    @Mapping(target = "weaknesses", ignore = true)
    @Mapping(target = "aiRecommendation", ignore = true)
    ResumeMatchResponse toResponse(ResumeMatch resumeMatch);

    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", ignore = true)
    ResumeMatchRankingItemResponse toRankingItemResponse(ResumeMatch resumeMatch);

    CandidateAnalysisItemResponse toCandidateAnalysisItemResponse(ResumeMatch resumeMatch);

    MatchedSkillResponse toResponse(SkillMatch skillMatch);
    List<MatchedSkillResponse> toMatchedSkillResponseList(List<SkillMatch> skillMatches);

    MissingSkillResponse toResponse(MissingSkill missingSkill);
    List<MissingSkillResponse> toMissingSkillResponseList(List<MissingSkill> missingSkills);

    StrengthWeaknessResponse toResponse(ResumeMatchNote note);
    List<StrengthWeaknessResponse> toNoteResponseList(List<ResumeMatchNote> notes);

    AiRecommendationResponse toResponse(AiRecommendation aiRecommendation);
}
