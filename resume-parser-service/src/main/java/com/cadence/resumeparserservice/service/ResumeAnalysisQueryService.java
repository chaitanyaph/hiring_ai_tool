package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Read-only side backing the Resume Analysis Dashboard, ranking table, recommendations feed, and drawer sub-sections. */
public interface ResumeAnalysisQueryService {

    PagedResponse<ResumeMatchRankingItemResponse> getRanking(UUID jobId, String search, Pageable pageable);

    ResumeMatchSummaryResponse getSummary(UUID jobId);

    List<ResumeMatchRankingItemResponse> getTop(UUID jobId);

    List<ResumeMatchRankingItemResponse> getRecommendations(UUID jobId, UUID departmentId, Integer minScore, Pageable pageable);

    ResumeMatchResponse getByApplicationId(UUID applicationId);

    List<CandidateAnalysisItemResponse> getByCandidateId(UUID candidateId);

    List<MatchedSkillResponse> getSkills(UUID analysisId);

    List<MissingSkillResponse> getMissingSkills(UUID analysisId);

    List<StrengthWeaknessResponse> getStrengths(UUID analysisId);

    List<StrengthWeaknessResponse> getWeaknesses(UUID analysisId);

    AiRecommendationResponse getRecommendation(UUID analysisId);

    /** Score/label breakdown only, no skills/notes lists -- the drawer's compact "summary" sub-section. */
    ResumeMatchResponse getScoreSummary(UUID analysisId);
}
