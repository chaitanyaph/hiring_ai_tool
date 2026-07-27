package com.cadence.candidateservice.service.impl;

import com.cadence.candidateservice.config.RedisConfig;
import com.cadence.candidateservice.constant.ApplicationStatus;
import com.cadence.candidateservice.dto.response.DashboardResponse;
import com.cadence.candidateservice.entity.Candidate;
import com.cadence.candidateservice.mapper.ApplicationMapper;
import com.cadence.candidateservice.mapper.SavedJobMapper;
import com.cadence.candidateservice.repository.ApplicationRepository;
import com.cadence.candidateservice.repository.CandidateRepository;
import com.cadence.candidateservice.repository.SavedJobRepository;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * profileViews and upcomingInterviewsCount are deliberately 0 -- no
 * Notification/Interview Scheduling service exists yet to source those
 * numbers from, and this service never fabricates data it doesn't own.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Set<ApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            ApplicationStatus.APPLIED, ApplicationStatus.RESUME_SCREENING, ApplicationStatus.AI_RESUME_MATCH,
            ApplicationStatus.AI_INTERVIEW, ApplicationStatus.CODING_ASSESSMENT,
            ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationStatus.HR_INTERVIEW);

    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final ApplicationMapper applicationMapper;
    private final SavedJobMapper savedJobMapper;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.DASHBOARD_CACHE, key = "#candidate.userId")
    public DashboardResponse getDashboard(CurrentUser candidate) {
        Candidate profile = candidateRepository.findById(candidate.getUserId()).orElse(null);

        return DashboardResponse.builder()
                .profileCompletionPercent(profile != null ? profile.getProfileCompletionPercent() : 0)
                .aiResumeScore(profile != null ? profile.getAiResumeScore() : null)
                .activeApplicationsCount(applicationRepository.countByCandidateIdAndStatusIn(candidate.getUserId(), ACTIVE_STATUSES))
                .savedJobsCount(savedJobRepository.countByCandidateId(candidate.getUserId()))
                .profileViews(0)
                .upcomingInterviewsCount(0)
                .recentApplications(applicationMapper.toResponseList(
                        applicationRepository.findTop5ByCandidateIdOrderByAppliedAtDesc(candidate.getUserId())))
                .savedJobs(savedJobMapper.toResponseList(
                        savedJobRepository.findAllByCandidateIdOrderBySavedAtDesc(candidate.getUserId()).stream().limit(5).toList()))
                .build();
    }
}
