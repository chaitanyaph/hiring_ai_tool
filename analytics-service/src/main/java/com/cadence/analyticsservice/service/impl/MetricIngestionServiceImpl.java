package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.entity.AnalyticsActivityLog;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.entity.RecruiterPerformanceSnapshot;
import com.cadence.analyticsservice.kafka.event.*;
import com.cadence.analyticsservice.repository.AnalyticsActivityLogRepository;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.repository.RecruiterPerformanceSnapshotRepository;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The core aggregation engine. Every handler is a pure increment into
 * metric_snapshot -- never a read-modify-scan of raw event history.
 *
 * Scope limitation, flagged (also in README): many granular events
 * (resume/AI-interview/coding/interview-eval scores, offer lifecycle)
 * do not carry companyId anywhere in the platform -- enriching every
 * single Kafka message with a Feign call to attribute it to a company
 * would be a serious anti-pattern for "scalable analytics aggregation"
 * at high event volume, so these are tracked at GLOBAL scope only.
 * Metrics that DO carry companyId directly on the event (company/job/
 * application volumes, and critically the whole funnel via
 * ApplicationStatusChangedEvent) are tracked at both COMPANY and
 * GLOBAL scope.
 */
@Service
@RequiredArgsConstructor
public class MetricIngestionServiceImpl implements MetricIngestionService {

    private final MetricSnapshotRepository metricSnapshotRepository;
    private final RecruiterPerformanceSnapshotRepository recruiterPerformanceSnapshotRepository;
    private final AnalyticsActivityLogRepository analyticsActivityLogRepository;

    @Override
    @Transactional
    public void onCompanyCreated(CompanyCreatedEvent event) {
        incrementGlobal(MetricKey.TOTAL_COMPANIES, BigDecimal.ONE);
        incrementGlobal(MetricKey.ACTIVE_COMPANIES, BigDecimal.ONE);
        log("company-service", "CompanyCreated", event.getCompanyId());
    }

    @Override
    @Transactional
    public void onJobPublished(JobPublishedEvent event) {
        incrementBoth(event.getCompanyId(), MetricKey.TOTAL_JOBS, BigDecimal.ONE);
        incrementBoth(event.getCompanyId(), MetricKey.PUBLISHED_JOBS, BigDecimal.ONE);
        log("job-service", "JobPublished", event.getJobId());
    }

    @Override
    @Transactional
    public void onJobClosed(JobClosedEvent event) {
        incrementBoth(event.getCompanyId(), MetricKey.CLOSED_JOBS, BigDecimal.ONE);
        log("job-service", "JobClosed", event.getJobId());
    }

    @Override
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        if (!"CANDIDATE".equalsIgnoreCase(event.getUserType())) {
            return;
        }
        incrementGlobal(MetricKey.CANDIDATES_REGISTERED, BigDecimal.ONE);
        if (event.getCompanyId() != null) {
            incrementCompany(event.getCompanyId(), MetricKey.CANDIDATES_REGISTERED, BigDecimal.ONE);
        }
        log("auth-service", "UserRegistered", event.getUserId());
    }

    @Override
    @Transactional
    public void onApplicationCreated(ApplicationCreatedEvent event) {
        incrementBoth(event.getCompanyId(), MetricKey.TOTAL_APPLICATIONS, BigDecimal.ONE);
        log("application-service", "ApplicationCreated", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        String stage = event.getToStatus();
        incrementCompanyDimension(event.getCompanyId(), MetricKey.FUNNEL_STAGE, stage, PeriodType.ALL_TIME,
                PeriodType.ALL_TIME_DATE, BigDecimal.ONE);
        incrementGlobalDimension(MetricKey.FUNNEL_STAGE, stage, PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE, BigDecimal.ONE);

        if ("HIRED".equalsIgnoreCase(stage)) {
            incrementBoth(event.getCompanyId(), MetricKey.HIRES, BigDecimal.ONE);
            LocalDate monthBucket = LocalDate.now().withDayOfMonth(1);
            incrementCompanyDimension(event.getCompanyId(), MetricKey.HIRES, "", PeriodType.MONTHLY, monthBucket, BigDecimal.ONE);
        }
        log("application-service", "ApplicationStatusChanged", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onRecruiterAssigned(RecruiterAssignedEvent event) {
        RecruiterPerformanceSnapshot snapshot = recruiterPerformanceSnapshotRepository
                .findByRecruiterIdAndPeriodDate(event.getRecruiterId(), PeriodType.ALL_TIME_DATE)
                .orElseGet(() -> RecruiterPerformanceSnapshot.builder()
                        .recruiterId(event.getRecruiterId()).companyId(event.getCompanyId())
                        .periodDate(PeriodType.ALL_TIME_DATE).build());
        snapshot.setApplicationsReviewed(snapshot.getApplicationsReviewed() + 1);
        snapshot.setUpdatedAt(LocalDateTime.now());
        recruiterPerformanceSnapshotRepository.save(snapshot);
        log("application-service", "RecruiterAssigned", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onResumeParsed(ResumeParsedEvent event) {
        incrementGlobal(MetricKey.RESUMES_PARSED, BigDecimal.ONE);
        incrementGlobal(MetricKey.RESUME_PARSE_SUCCESS, BigDecimal.ONE);
        log("resume-parser-service", "ResumeParsed", event.getResumeId());
    }

    @Override
    @Transactional
    public void onResumeAnalyzed(ResumeAnalyzedEvent event) {
        if (event.getOverallMatchScore() != null) {
            incrementGlobal(MetricKey.RESUME_SCORE_SUM, BigDecimal.valueOf(event.getOverallMatchScore()));
            incrementGlobal(MetricKey.RESUME_SCORE_COUNT, BigDecimal.ONE);
        }
        log("resume-parser-service", "ResumeAnalyzed", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onCandidateShortlisted(CandidateShortlistedEvent event) {
        incrementGlobalDimension(MetricKey.CANDIDATE_SHORTLISTED_COUNT, nvl(event.getDecision()), PeriodType.ALL_TIME,
                PeriodType.ALL_TIME_DATE, BigDecimal.ONE);
        log("ai-interview-service", "CandidateShortlisted", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onInterviewEvaluated(InterviewEvaluatedEvent event) {
        if (event.getOverallScore() != null) {
            incrementGlobal(MetricKey.AI_INTERVIEW_SCORE_SUM, BigDecimal.valueOf(event.getOverallScore()));
            incrementGlobal(MetricKey.AI_INTERVIEW_SCORE_COUNT, BigDecimal.ONE);
        }
        log("ai-interview-service", "InterviewEvaluated", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        incrementGlobal(MetricKey.CODING_ASSESSMENT_COMPLETED_COUNT, BigDecimal.ONE);
        if (event.getScore() != null) {
            incrementGlobal(MetricKey.CODING_ASSESSMENT_SCORE_SUM, BigDecimal.valueOf(event.getScore()));
            incrementGlobal(MetricKey.CODING_ASSESSMENT_SCORE_COUNT, BigDecimal.ONE);
        }
        log("coding-assessment-service", "CodingAssessmentCompleted", event.getApplicationId());
    }

    @Override
    @Transactional
    public void onInterviewCompleted(InterviewManagementInterviewCompletedEvent event) {
        incrementGlobal(MetricKey.INTERVIEW_COMPLETED_COUNT, BigDecimal.ONE);
        if (event.getOverallRating() != null && event.getRoundType() != null) {
            BigDecimal rating = BigDecimal.valueOf(event.getOverallRating());
            if ("TECHNICAL".equalsIgnoreCase(event.getRoundType())) {
                incrementGlobal(MetricKey.TECHNICAL_INTERVIEW_SCORE_SUM, rating);
                incrementGlobal(MetricKey.TECHNICAL_INTERVIEW_SCORE_COUNT, BigDecimal.ONE);
            } else if ("HR".equalsIgnoreCase(event.getRoundType())) {
                incrementGlobal(MetricKey.HR_INTERVIEW_SCORE_SUM, rating);
                incrementGlobal(MetricKey.HR_INTERVIEW_SCORE_COUNT, BigDecimal.ONE);
            }
        }
        log("interview-management-service", "InterviewCompleted", event.getInterviewId());
    }

    @Override
    @Transactional
    public void onInterviewCancelled(InterviewManagementInterviewCancelledEvent event) {
        incrementGlobal(MetricKey.INTERVIEW_CANCELLED_COUNT, BigDecimal.ONE);
        log("interview-management-service", "InterviewCancelled", event.getInterviewId());
    }

    @Override
    @Transactional
    public void onOfferGenerated(OfferGeneratedEvent event) {
        incrementGlobal(MetricKey.OFFERS_GENERATED, BigDecimal.ONE);
        log("offer-management-service", "OfferGenerated", event.getOfferId());
    }

    @Override
    @Transactional
    public void onOfferSent(OfferSentEvent event) {
        incrementGlobal(MetricKey.OFFERS_SENT, BigDecimal.ONE);
        log("offer-management-service", "OfferSent", event.getOfferId());
    }

    @Override
    @Transactional
    public void onOfferAccepted(OfferAcceptedEvent event) {
        incrementGlobal(MetricKey.OFFERS_ACCEPTED, BigDecimal.ONE);
        log("offer-management-service", "OfferAccepted", event.getOfferId());
    }

    @Override
    @Transactional
    public void onOfferRejected(OfferRejectedEvent event) {
        incrementGlobal(MetricKey.OFFERS_REJECTED, BigDecimal.ONE);
        log("offer-management-service", "OfferRejected", event.getOfferId());
    }

    @Override
    @Transactional
    public void onOfferNegotiationRequested(OfferNegotiationRequestedEvent event) {
        incrementGlobal(MetricKey.OFFERS_NEGOTIATION_REQUESTED, BigDecimal.ONE);
        log("offer-management-service", "OfferNegotiationRequested", event.getOfferId());
    }

    // ---- generic increment helpers ----

    private void incrementBoth(UUID companyId, MetricKey key, BigDecimal delta) {
        incrementCompany(companyId, key, delta);
        incrementGlobal(key, delta);
    }

    private void incrementGlobal(MetricKey key, BigDecimal delta) {
        incrementGlobalDimension(key, "", PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE, delta);
    }

    private void incrementCompany(UUID companyId, MetricKey key, BigDecimal delta) {
        incrementCompanyDimension(companyId, key, "", PeriodType.ALL_TIME, PeriodType.ALL_TIME_DATE, delta);
    }

    private void incrementGlobalDimension(MetricKey key, String dimension, PeriodType periodType, LocalDate periodDate, BigDecimal delta) {
        increment(MetricScope.GLOBAL, MetricScope.NO_SCOPE_ID, key, dimension, periodType, periodDate, delta);
    }

    private void incrementCompanyDimension(UUID companyId, MetricKey key, String dimension, PeriodType periodType, LocalDate periodDate, BigDecimal delta) {
        increment(MetricScope.COMPANY, companyId, key, dimension, periodType, periodDate, delta);
    }

    private void increment(MetricScope scope, UUID scopeId, MetricKey key, String dimension, PeriodType periodType, LocalDate periodDate, BigDecimal delta) {
        MetricSnapshot snapshot = metricSnapshotRepository
                .findByScopeAndScopeIdAndMetricKeyAndDimensionAndPeriodTypeAndPeriodDate(scope, scopeId, key, dimension, periodType, periodDate)
                .orElseGet(() -> MetricSnapshot.builder()
                        .scope(scope).scopeId(scopeId).metricKey(key).dimension(dimension)
                        .periodType(periodType).periodDate(periodDate).metricValue(BigDecimal.ZERO).build());
        snapshot.setMetricValue(snapshot.getMetricValue().add(delta));
        snapshot.setUpdatedAt(LocalDateTime.now());
        metricSnapshotRepository.save(snapshot);
    }

    private void log(String source, String eventType, UUID relatedEntityId) {
        analyticsActivityLogRepository.save(AnalyticsActivityLog.builder()
                .source(source).eventType(eventType).message(eventType + " processed").relatedEntityId(relatedEntityId).build());
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}
