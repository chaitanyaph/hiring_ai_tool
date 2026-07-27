package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.dto.response.PagedResponse;
import com.cadence.aiinterviewservice.dto.response.ShortlistItemResponse;
import com.cadence.aiinterviewservice.dto.response.ShortlistSummaryResponse;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.feign.ApplicationServiceClient;
import com.cadence.aiinterviewservice.feign.dto.ApplicationSummaryDto;
import com.cadence.aiinterviewservice.mapper.ShortlistMapper;
import com.cadence.aiinterviewservice.repository.CandidateShortlistRepository;
import com.cadence.aiinterviewservice.service.ShortlistQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShortlistQueryServiceImpl implements ShortlistQueryService {

    private static final int RANKING_LIMIT = 100;

    private final CandidateShortlistRepository candidateShortlistRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final ShortlistMapper shortlistMapper;

    @Override
    public PagedResponse<ShortlistItemResponse> getShortlisted(UUID jobId, Pageable pageable) {
        return toPagedResponse(jobId, ShortlistDecision.SHORTLISTED, pageable);
    }

    @Override
    public PagedResponse<ShortlistItemResponse> getRejected(UUID jobId, Pageable pageable) {
        return toPagedResponse(jobId, ShortlistDecision.REJECTED, pageable);
    }

    @Override
    public PagedResponse<ShortlistItemResponse> getManualReview(UUID jobId, Pageable pageable) {
        return toPagedResponse(jobId, ShortlistDecision.MANUAL_REVIEW, pageable);
    }

    @Override
    public ShortlistSummaryResponse getSummary(UUID jobId) {
        long shortlisted = candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.SHORTLISTED);
        long rejected = candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.REJECTED);
        long manualReview = candidateShortlistRepository.countByJobIdAndDecision(jobId, ShortlistDecision.MANUAL_REVIEW);
        long total = shortlisted + rejected + manualReview;
        double rate = total == 0 ? 0.0 : (shortlisted * 100.0) / total;

        return ShortlistSummaryResponse.builder()
                .shortlistedCount(shortlisted)
                .rejectedCount(rejected)
                .manualReviewCount(manualReview)
                .autoShortlistRatePercent(Math.round(rate * 10.0) / 10.0)
                .build();
    }

    @Override
    public List<ShortlistItemResponse> getRanking(UUID jobId) {
        Page<CandidateShortlist> page = candidateShortlistRepository.findAllByJobIdAndDecisionOrderByOverallMatchScoreDesc(
                jobId, ShortlistDecision.SHORTLISTED, PageRequest.of(0, RANKING_LIMIT));
        Map<UUID, ApplicationSummaryDto> applications = fetchApplicationsByJob(jobId);
        return page.getContent().stream().map(s -> enrich(s, applications)).toList();
    }

    private PagedResponse<ShortlistItemResponse> toPagedResponse(UUID jobId, ShortlistDecision decision, Pageable pageable) {
        Page<CandidateShortlist> page = candidateShortlistRepository.findAllByJobIdAndDecisionOrderByOverallMatchScoreDesc(jobId, decision, pageable);
        Map<UUID, ApplicationSummaryDto> applications = fetchApplicationsByJob(jobId);
        return PagedResponse.from(page.map(s -> enrich(s, applications)));
    }

    private Map<UUID, ApplicationSummaryDto> fetchApplicationsByJob(UUID jobId) {
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return Map.of();
        }
        return applications.stream().collect(java.util.stream.Collectors.toMap(ApplicationSummaryDto::getId, Function.identity(), (a, b) -> a));
    }

    private ShortlistItemResponse enrich(CandidateShortlist shortlist, Map<UUID, ApplicationSummaryDto> applications) {
        ShortlistItemResponse response = shortlistMapper.toResponse(shortlist);
        ApplicationSummaryDto application = applications.get(shortlist.getApplicationId());
        if (application != null) {
            response.setFullName(application.getCandidateNameSnapshot());
            response.setEmail(application.getCandidateEmailSnapshot());
            response.setJobTitle(application.getJobTitleSnapshot());
        }
        return response;
    }
}
