package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.dto.response.AssessmentAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.CandidateAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.InterviewAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.JobAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.OfferAnalyticsResponse;
import com.cadence.analyticsservice.dto.response.ResumeAnalyticsResponse;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.service.DomainAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DomainAnalyticsServiceImpl implements DomainAnalyticsService {

    private final MetricSnapshotRepository metricSnapshotRepository;

    @Override
    public JobAnalyticsResponse getJobAnalytics(UUID companyId) {
        Map<MetricKey, Long> values = fetchScoped(companyId,
                List.of(MetricKey.TOTAL_JOBS, MetricKey.PUBLISHED_JOBS, MetricKey.CLOSED_JOBS));
        return JobAnalyticsResponse.builder()
                .totalJobs(values.getOrDefault(MetricKey.TOTAL_JOBS, 0L))
                .publishedJobs(values.getOrDefault(MetricKey.PUBLISHED_JOBS, 0L))
                .closedJobs(values.getOrDefault(MetricKey.CLOSED_JOBS, 0L))
                .build();
    }

    @Override
    public CandidateAnalyticsResponse getCandidateAnalytics(UUID companyId) {
        Map<MetricKey, Long> scoped = fetchScoped(companyId,
                List.of(MetricKey.CANDIDATES_REGISTERED, MetricKey.TOTAL_APPLICATIONS));
        // CANDIDATE_SHORTLISTED_COUNT is ingested at GLOBAL scope only -- the shortlisting
        // event carries no companyId -- so this is always platform-wide, flagged.
        long shortlisted = sumDimensions(MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID, MetricKey.CANDIDATE_SHORTLISTED_COUNT);
        return CandidateAnalyticsResponse.builder()
                .candidatesRegistered(scoped.getOrDefault(MetricKey.CANDIDATES_REGISTERED, 0L))
                .totalApplications(scoped.getOrDefault(MetricKey.TOTAL_APPLICATIONS, 0L))
                .shortlistedCount(shortlisted)
                .build();
    }

    @Override
    public ResumeAnalyticsResponse getResumeAnalytics() {
        Map<MetricKey, Long> g = fetchGlobal(List.of(MetricKey.RESUMES_PARSED, MetricKey.RESUME_PARSE_SUCCESS,
                MetricKey.RESUME_PARSE_FAILURE, MetricKey.RESUME_SCORE_SUM, MetricKey.RESUME_SCORE_COUNT));
        return ResumeAnalyticsResponse.builder()
                .resumesParsed(g.getOrDefault(MetricKey.RESUMES_PARSED, 0L))
                .parseSuccessCount(g.getOrDefault(MetricKey.RESUME_PARSE_SUCCESS, 0L))
                .parseFailureCount(g.getOrDefault(MetricKey.RESUME_PARSE_FAILURE, 0L))
                .avgMatchScore(average(g.get(MetricKey.RESUME_SCORE_SUM), g.get(MetricKey.RESUME_SCORE_COUNT)))
                .build();
    }

    @Override
    public InterviewAnalyticsResponse getInterviewAnalytics() {
        Map<MetricKey, Long> g = fetchGlobal(List.of(MetricKey.INTERVIEW_COMPLETED_COUNT, MetricKey.INTERVIEW_CANCELLED_COUNT,
                MetricKey.AI_INTERVIEW_SCORE_SUM, MetricKey.AI_INTERVIEW_SCORE_COUNT,
                MetricKey.TECHNICAL_INTERVIEW_SCORE_SUM, MetricKey.TECHNICAL_INTERVIEW_SCORE_COUNT,
                MetricKey.HR_INTERVIEW_SCORE_SUM, MetricKey.HR_INTERVIEW_SCORE_COUNT));
        long completed = g.getOrDefault(MetricKey.INTERVIEW_COMPLETED_COUNT, 0L);
        long cancelled = g.getOrDefault(MetricKey.INTERVIEW_CANCELLED_COUNT, 0L);
        return InterviewAnalyticsResponse.builder()
                .interviewsCompleted(completed)
                .interviewsCancelled(cancelled)
                .completionRatePercent(percentOf(completed, completed + cancelled))
                .avgAiInterviewScore(average(g.get(MetricKey.AI_INTERVIEW_SCORE_SUM), g.get(MetricKey.AI_INTERVIEW_SCORE_COUNT)))
                .avgTechnicalScore(average(g.get(MetricKey.TECHNICAL_INTERVIEW_SCORE_SUM), g.get(MetricKey.TECHNICAL_INTERVIEW_SCORE_COUNT)))
                .avgHrScore(average(g.get(MetricKey.HR_INTERVIEW_SCORE_SUM), g.get(MetricKey.HR_INTERVIEW_SCORE_COUNT)))
                .build();
    }

    @Override
    public AssessmentAnalyticsResponse getAssessmentAnalytics() {
        Map<MetricKey, Long> g = fetchGlobal(List.of(MetricKey.CODING_ASSESSMENT_COMPLETED_COUNT,
                MetricKey.CODING_ASSESSMENT_SCORE_SUM, MetricKey.CODING_ASSESSMENT_SCORE_COUNT));
        return AssessmentAnalyticsResponse.builder()
                .assessmentsCompleted(g.getOrDefault(MetricKey.CODING_ASSESSMENT_COMPLETED_COUNT, 0L))
                .avgScore(average(g.get(MetricKey.CODING_ASSESSMENT_SCORE_SUM), g.get(MetricKey.CODING_ASSESSMENT_SCORE_COUNT)))
                .build();
    }

    @Override
    public OfferAnalyticsResponse getOfferAnalytics() {
        Map<MetricKey, Long> g = fetchGlobal(List.of(MetricKey.OFFERS_GENERATED, MetricKey.OFFERS_SENT,
                MetricKey.OFFERS_ACCEPTED, MetricKey.OFFERS_REJECTED, MetricKey.OFFERS_NEGOTIATION_REQUESTED));
        long sent = g.getOrDefault(MetricKey.OFFERS_SENT, 0L);
        long accepted = g.getOrDefault(MetricKey.OFFERS_ACCEPTED, 0L);
        long rejected = g.getOrDefault(MetricKey.OFFERS_REJECTED, 0L);
        long negotiation = g.getOrDefault(MetricKey.OFFERS_NEGOTIATION_REQUESTED, 0L);
        return OfferAnalyticsResponse.builder()
                .offersGenerated(g.getOrDefault(MetricKey.OFFERS_GENERATED, 0L))
                .offersSent(sent)
                .offersAccepted(accepted)
                .offersRejected(rejected)
                .negotiationRequestedCount(negotiation)
                .acceptanceRatePercent(percentOf(accepted, accepted + rejected))
                .negotiationRatePercent(percentOf(negotiation, sent))
                .build();
    }

    private Map<MetricKey, Long> fetchScoped(UUID companyId, List<MetricKey> keys) {
        MetricScope scope = companyId != null ? MetricScope.COMPANY : MetricScope.GLOBAL;
        UUID scopeId = companyId != null ? companyId : MetricScope.NO_SCOPE_ID;
        return fetch(scope, scopeId, keys);
    }

    private Map<MetricKey, Long> fetchGlobal(List<MetricKey> keys) {
        return fetch(MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID, keys);
    }

    private Map<MetricKey, Long> fetch(MetricScope scope, UUID scopeId, List<MetricKey> keys) {
        return metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyInAndPeriodTypeAndPeriodDate(scope, scopeId, keys, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE)
                .stream()
                .collect(Collectors.toMap(MetricSnapshot::getMetricKey, s -> s.getMetricValue().longValue()));
    }

    private long sumDimensions(MetricScope scope, UUID scopeId, MetricKey key) {
        return metricSnapshotRepository
                .findAllByScopeAndScopeIdAndMetricKeyAndPeriodTypeAndPeriodDate(scope, scopeId, key, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE)
                .stream()
                .mapToLong(s -> s.getMetricValue().longValue())
                .sum();
    }

    private Double average(Long sum, Long count) {
        if (sum == null || count == null || count == 0) {
            return null;
        }
        return round2(sum.doubleValue() / count);
    }

    private Double percentOf(long part, long whole) {
        if (whole == 0) {
            return null;
        }
        return round2(part * 100.0 / whole);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
