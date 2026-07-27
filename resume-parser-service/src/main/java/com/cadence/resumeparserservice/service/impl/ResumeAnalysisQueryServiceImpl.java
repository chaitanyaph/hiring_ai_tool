package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.NoteType;
import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.entity.ResumeMatch;
import com.cadence.resumeparserservice.exception.ErrorCode;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.mapper.ResumeMatchMapper;
import com.cadence.resumeparserservice.repository.*;
import com.cadence.resumeparserservice.service.ResumeAnalysisQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeAnalysisQueryServiceImpl implements ResumeAnalysisQueryService {

    private static final int BELOW_THRESHOLD_SCORE = 50;

    private final ResumeMatchRepository resumeMatchRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final SkillMatchRepository skillMatchRepository;
    private final MissingSkillRepository missingSkillRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final ResumeMatchNoteRepository resumeMatchNoteRepository;
    private final ResumeMatchMapper mapper;

    @Override
    public PagedResponse<ResumeMatchRankingItemResponse> getRanking(UUID jobId, String search, Pageable pageable) {
        Page<ResumeMatchRankingItemResponse> page = resumeMatchRepository.searchByJob(jobId, search, pageable)
                .map(this::toRankingItem);
        return PagedResponse.from(page);
    }

    @Override
    public ResumeMatchSummaryResponse getSummary(UUID jobId) {
        long analyzed = resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.ANALYZED);
        long failed = resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.FAILED);
        long awaitingResume = resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.AWAITING_RESUME);
        long awaitingParse = resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.AWAITING_PARSE);
        long analyzing = resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.ANALYZING);

        return ResumeMatchSummaryResponse.builder()
                .totalCount(analyzed + failed + awaitingResume + awaitingParse + analyzing)
                .analyzedCount(analyzed)
                .awaitingCount(awaitingResume + awaitingParse + analyzing)
                .failedCount(failed)
                .averageMatchScore(resumeMatchRepository.findAvgMatchScoreByJob(jobId))
                .topMatchScore(resumeMatchRepository.findTopScoreByJob(jobId))
                .belowThresholdCount(resumeMatchRepository.countByJobIdAndStatusAndOverallMatchScoreLessThan(
                        jobId, ResumeMatchStatus.ANALYZED, BELOW_THRESHOLD_SCORE))
                .build();
    }

    @Override
    public List<ResumeMatchRankingItemResponse> getTop(UUID jobId) {
        return resumeMatchRepository.findTop10ByJobIdAndStatusOrderByOverallMatchScoreDesc(
                        jobId, ResumeMatchStatus.ANALYZED)
                .stream().map(this::toRankingItem).toList();
    }

    @Override
    public List<ResumeMatchRankingItemResponse> getRecommendations(UUID jobId, UUID departmentId, Integer minScore, Pageable pageable) {
        return resumeMatchRepository.findRecommendations(jobId, departmentId, minScore, pageable)
                .stream().map(this::toRankingItem).toList();
    }

    @Override
    public ResumeMatchResponse getByApplicationId(UUID applicationId) {
        ResumeMatch resumeMatch = resumeMatchRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MATCH_NOT_FOUND, "No resume match found for application " + applicationId));
        return toFullResponse(resumeMatch);
    }

    @Override
    public List<CandidateAnalysisItemResponse> getByCandidateId(UUID candidateId) {
        return resumeMatchRepository.findAllByCandidateIdAndStatusOrderByOverallMatchScoreDesc(
                        candidateId, ResumeMatchStatus.ANALYZED)
                .stream().map(mapper::toCandidateAnalysisItemResponse).toList();
    }

    @Override
    public List<MatchedSkillResponse> getSkills(UUID analysisId) {
        return mapper.toMatchedSkillResponseList(skillMatchRepository.findAllByResumeMatchId(analysisId));
    }

    @Override
    public List<MissingSkillResponse> getMissingSkills(UUID analysisId) {
        return mapper.toMissingSkillResponseList(missingSkillRepository.findAllByResumeMatchId(analysisId));
    }

    @Override
    public List<StrengthWeaknessResponse> getStrengths(UUID analysisId) {
        return mapper.toNoteResponseList(resumeMatchNoteRepository.findAllByResumeMatchIdAndNoteTypeOrderByDisplayOrderAsc(
                analysisId, NoteType.STRENGTH));
    }

    @Override
    public List<StrengthWeaknessResponse> getWeaknesses(UUID analysisId) {
        return mapper.toNoteResponseList(resumeMatchNoteRepository.findAllByResumeMatchIdAndNoteTypeOrderByDisplayOrderAsc(
                analysisId, NoteType.WEAKNESS));
    }

    @Override
    public AiRecommendationResponse getRecommendation(UUID analysisId) {
        return aiRecommendationRepository.findByResumeMatchId(analysisId)
                .map(mapper::toResponse)
                .orElse(null);
    }

    @Override
    public ResumeMatchResponse getScoreSummary(UUID analysisId) {
        ResumeMatch resumeMatch = resumeMatchRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MATCH_NOT_FOUND, "No resume match found: " + analysisId));
        return enrichWithCandidate(mapper.toResponse(resumeMatch), resumeMatch);
    }

    private ResumeMatchResponse toFullResponse(ResumeMatch resumeMatch) {
        ResumeMatchResponse response = enrichWithCandidate(mapper.toResponse(resumeMatch), resumeMatch);
        UUID id = resumeMatch.getId();
        response.setMatchedSkills(mapper.toMatchedSkillResponseList(skillMatchRepository.findAllByResumeMatchId(id)));
        response.setMissingSkills(mapper.toMissingSkillResponseList(missingSkillRepository.findAllByResumeMatchId(id)));
        response.setStrengths(getStrengths(id));
        response.setWeaknesses(getWeaknesses(id));
        response.setAiRecommendation(getRecommendation(id));
        return response;
    }

    private ResumeMatchResponse enrichWithCandidate(ResumeMatchResponse response, ResumeMatch resumeMatch) {
        if (resumeMatch.getParsedResumeId() != null) {
            Optional<ParsedResume> parsedResume = parsedResumeRepository.findById(resumeMatch.getParsedResumeId());
            parsedResume.ifPresent(p -> {
                response.setFullName(p.getFullName());
                response.setEmail(p.getEmail());
                response.setProfessionalSummary(p.getProfessionalSummary());
            });
        }
        return response;
    }

    private ResumeMatchRankingItemResponse toRankingItem(ResumeMatch resumeMatch) {
        ResumeMatchRankingItemResponse item = mapper.toRankingItemResponse(resumeMatch);
        if (resumeMatch.getParsedResumeId() != null) {
            parsedResumeRepository.findById(resumeMatch.getParsedResumeId()).ifPresent(p -> {
                item.setFullName(p.getFullName());
                item.setEmail(p.getEmail());
            });
        }
        return item;
    }
}
