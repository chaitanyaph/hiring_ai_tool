package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.dto.response.ShortlistItemResponse;
import com.cadence.aiinterviewservice.dto.response.ShortlistSummaryResponse;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.feign.ApplicationServiceClient;
import com.cadence.aiinterviewservice.feign.dto.ApplicationSummaryDto;
import com.cadence.aiinterviewservice.feign.dto.FeignApiResponse;
import com.cadence.aiinterviewservice.mapper.ShortlistMapper;
import com.cadence.aiinterviewservice.repository.CandidateShortlistRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortlistQueryServiceImplTest {

    @Mock private CandidateShortlistRepository candidateShortlistRepository;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private ShortlistMapper shortlistMapper;

    @InjectMocks
    private ShortlistQueryServiceImpl shortlistQueryService;

    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
    }

    @Test
    void getSummary_shouldComputeAutoShortlistRate() {
        when(candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.SHORTLISTED)).thenReturn(6L);
        when(candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.REJECTED)).thenReturn(3L);
        when(candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.MANUAL_REVIEW)).thenReturn(1L);

        ShortlistSummaryResponse summary = shortlistQueryService.getSummary(jobId);

        assertThat(summary.getShortlistedCount()).isEqualTo(6);
        assertThat(summary.getRejectedCount()).isEqualTo(3);
        assertThat(summary.getManualReviewCount()).isEqualTo(1);
        assertThat(summary.getAutoShortlistRatePercent()).isEqualTo(60.0);
    }

    @Test
    void getSummary_shouldReturnZeroRate_whenNoCandidatesYet() {
        when(candidateShortlistRepository.countByJobIdAndDecision(any(), any())).thenReturn(0L);

        ShortlistSummaryResponse summary = shortlistQueryService.getSummary(jobId);

        assertThat(summary.getAutoShortlistRatePercent()).isEqualTo(0.0);
    }

    @Test
    void getRanking_shouldEnrichEachRowWithApplicationSnapshotFields() {
        UUID applicationId = UUID.randomUUID();
        CandidateShortlist shortlist = CandidateShortlist.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(UUID.randomUUID())
                .decision(ShortlistDecision.SHORTLISTED).overallMatchScore(88).build();
        Pageable pageable = PageRequest.of(0, 100);
        when(candidateShortlistRepository.findAllByJobIdAndDecisionOrderByOverallMatchScoreDesc(jobId, ShortlistDecision.SHORTLISTED, pageable))
                .thenReturn(new PageImpl<>(List.of(shortlist), pageable, 1));
        when(shortlistMapper.toResponse(shortlist)).thenReturn(
                ShortlistItemResponse.builder().applicationId(applicationId).overallMatchScore(88).build());

        ApplicationSummaryDto application = new ApplicationSummaryDto();
        application.setId(applicationId);
        application.setCandidateNameSnapshot("Jane Doe");
        application.setCandidateEmailSnapshot("jane@example.com");
        application.setJobTitleSnapshot("Backend Engineer");
        when(applicationServiceClient.getApplicationsByJob(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", List.of(application)));

        List<ShortlistItemResponse> ranking = shortlistQueryService.getRanking(jobId);

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).getFullName()).isEqualTo("Jane Doe");
        assertThat(ranking.get(0).getEmail()).isEqualTo("jane@example.com");
        assertThat(ranking.get(0).getJobTitle()).isEqualTo("Backend Engineer");
    }
}
