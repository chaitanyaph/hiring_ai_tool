package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import com.cadence.resumeparserservice.dto.response.ResumeMatchRankingItemResponse;
import com.cadence.resumeparserservice.dto.response.ResumeMatchResponse;
import com.cadence.resumeparserservice.dto.response.ResumeMatchSummaryResponse;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.entity.ResumeMatch;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.mapper.ResumeMatchMapper;
import com.cadence.resumeparserservice.repository.*;
import com.cadence.resumeparserservice.service.impl.ResumeAnalysisQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisQueryServiceImplTest {

    @Mock private ResumeMatchRepository resumeMatchRepository;
    @Mock private ParsedResumeRepository parsedResumeRepository;
    @Mock private SkillMatchRepository skillMatchRepository;
    @Mock private MissingSkillRepository missingSkillRepository;
    @Mock private AiRecommendationRepository aiRecommendationRepository;
    @Mock private ResumeMatchNoteRepository resumeMatchNoteRepository;
    @Mock private ResumeMatchMapper mapper;

    @InjectMocks
    private ResumeAnalysisQueryServiceImpl service;

    private UUID jobId;
    private UUID parsedResumeId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        parsedResumeId = UUID.randomUUID();
    }

    @Test
    void getSummary_shouldAggregateCountsAcrossEveryStatus() {
        when(resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.ANALYZED)).thenReturn(5L);
        when(resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.FAILED)).thenReturn(1L);
        when(resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.AWAITING_RESUME)).thenReturn(2L);
        when(resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.AWAITING_PARSE)).thenReturn(1L);
        when(resumeMatchRepository.countByJobIdAndStatus(jobId, ResumeMatchStatus.ANALYZING)).thenReturn(1L);
        when(resumeMatchRepository.findAvgMatchScoreByJob(jobId)).thenReturn(72.5);
        when(resumeMatchRepository.findTopScoreByJob(jobId)).thenReturn(95);
        when(resumeMatchRepository.countByJobIdAndStatusAndOverallMatchScoreLessThan(jobId, ResumeMatchStatus.ANALYZED, 50)).thenReturn(2L);

        ResumeMatchSummaryResponse summary = service.getSummary(jobId);

        assertThat(summary.getAnalyzedCount()).isEqualTo(5);
        assertThat(summary.getFailedCount()).isEqualTo(1);
        assertThat(summary.getAwaitingCount()).isEqualTo(4);
        assertThat(summary.getTotalCount()).isEqualTo(10);
        assertThat(summary.getAverageMatchScore()).isEqualTo(72.5);
        assertThat(summary.getTopMatchScore()).isEqualTo(95);
        assertThat(summary.getBelowThresholdCount()).isEqualTo(2);
    }

    @Test
    void getRanking_shouldEnrichEachRowWithParsedResumeCandidateName() {
        ResumeMatch resumeMatch = ResumeMatch.builder()
                .applicationId(UUID.randomUUID()).jobId(jobId).candidateId(UUID.randomUUID())
                .parsedResumeId(parsedResumeId).status(ResumeMatchStatus.ANALYZED).overallMatchScore(88).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(resumeMatchRepository.searchByJob(jobId, null, pageable))
                .thenReturn(new PageImpl<>(List.of(resumeMatch), pageable, 1));
        when(mapper.toRankingItemResponse(resumeMatch)).thenReturn(
                ResumeMatchRankingItemResponse.builder().overallMatchScore(88).status(ResumeMatchStatus.ANALYZED).build());
        ParsedResume parsedResume = ParsedResume.builder().resumeId(UUID.randomUUID())
                .candidateId(resumeMatch.getCandidateId()).checksum("c")
                .fullName("Jane Doe").email("jane@example.com").build();
        when(parsedResumeRepository.findById(parsedResumeId)).thenReturn(Optional.of(parsedResume));

        var page = service.getRanking(jobId, null, pageable);

        ResumeMatchRankingItemResponse item = page.getContent().get(0);
        assertThat(item.getFullName()).isEqualTo("Jane Doe");
        assertThat(item.getEmail()).isEqualTo("jane@example.com");
        assertThat(item.getOverallMatchScore()).isEqualTo(88);
    }

    @Test
    void getByApplicationId_shouldThrow_whenNoMatchExists() {
        UUID applicationId = UUID.randomUUID();
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByApplicationId(applicationId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByApplicationId_shouldAssembleFullAggregateWithSubLists() {
        UUID applicationId = UUID.randomUUID();
        ResumeMatch resumeMatch = ResumeMatch.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(UUID.randomUUID())
                .parsedResumeId(parsedResumeId).status(ResumeMatchStatus.ANALYZED).overallMatchScore(70).build();
        when(resumeMatchRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(resumeMatch));
        when(mapper.toResponse(resumeMatch)).thenReturn(ResumeMatchResponse.builder().overallMatchScore(70).build());
        when(parsedResumeRepository.findById(parsedResumeId)).thenReturn(Optional.empty());
        when(skillMatchRepository.findAllByResumeMatchId(any())).thenReturn(List.of());
        when(missingSkillRepository.findAllByResumeMatchId(any())).thenReturn(List.of());
        when(resumeMatchNoteRepository.findAllByResumeMatchIdAndNoteTypeOrderByDisplayOrderAsc(any(), any())).thenReturn(List.of());
        when(aiRecommendationRepository.findByResumeMatchId(any())).thenReturn(Optional.empty());
        when(mapper.toMatchedSkillResponseList(any())).thenReturn(List.of());
        when(mapper.toMissingSkillResponseList(any())).thenReturn(List.of());
        when(mapper.toNoteResponseList(any())).thenReturn(List.of());

        var response = service.getByApplicationId(applicationId);

        assertThat(response.getOverallMatchScore()).isEqualTo(70);
        assertThat(response.getMatchedSkills()).isEmpty();
        assertThat(response.getMissingSkills()).isEmpty();
        assertThat(response.getAiRecommendation()).isNull();
    }
}
