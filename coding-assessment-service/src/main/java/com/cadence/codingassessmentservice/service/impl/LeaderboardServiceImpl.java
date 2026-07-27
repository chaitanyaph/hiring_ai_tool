package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.dto.response.CodingResultsSummaryResponse;
import com.cadence.codingassessmentservice.dto.response.LeaderboardItemResponse;
import com.cadence.codingassessmentservice.dto.response.PagedResponse;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.feign.ApplicationServiceClient;
import com.cadence.codingassessmentservice.feign.dto.ApplicationSummaryDto;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardServiceImpl implements LeaderboardService {

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ApplicationServiceClient applicationServiceClient;

    @Override
    public CodingResultsSummaryResponse getResultsSummary(UUID assessmentId) {
        long completed = candidateAssessmentRepository.countByAssessmentIdAndStatus(assessmentId, CandidateAssessmentStatus.COMPLETED);
        Double avgScore = candidateAssessmentRepository.findAvgScoreByAssessment(assessmentId);
        Integer maxScore = candidateAssessmentRepository.findMaxScoreByAssessment(assessmentId);
        Integer minScore = candidateAssessmentRepository.findMinScoreByAssessment(assessmentId);

        Map<UUID, ApplicationSummaryDto> applications = fetchApplications(assessmentId);
        String highestName = findCandidateNameByScore(assessmentId, maxScore, applications);
        String lowestName = findCandidateNameByScore(assessmentId, minScore, applications);

        return CodingResultsSummaryResponse.builder()
                .completedCount(completed)
                .avgScore(avgScore)
                .highestScore(maxScore)
                .highestScoreCandidateName(highestName)
                .lowestScore(minScore)
                .lowestScoreCandidateName(lowestName)
                .build();
    }

    @Override
    public List<LeaderboardItemResponse> getLeaderboard(UUID assessmentId) {
        List<CandidateAssessment> ranked = candidateAssessmentRepository
                .findAllByAssessmentIdAndStatusOrderByTotalScoreDescTimeUsedSecondsAsc(assessmentId, CandidateAssessmentStatus.COMPLETED);
        Map<UUID, ApplicationSummaryDto> applications = fetchApplications(assessmentId);

        List<LeaderboardItemResponse> result = new java.util.ArrayList<>();
        int rank = 1;
        for (CandidateAssessment ca : ranked) {
            ApplicationSummaryDto app = applications.get(ca.getApplicationId());
            result.add(LeaderboardItemResponse.builder()
                    .rank(rank++).applicationId(ca.getApplicationId())
                    .candidateName(app != null ? app.getCandidateNameSnapshot() : null)
                    .jobTitle(app != null ? app.getJobTitleSnapshot() : null)
                    .score(ca.getTotalScore()).timeUsedSeconds(ca.getTimeUsedSeconds())
                    .completedAt(ca.getCompletedAt())
                    .build());
        }
        return result;
    }

    @Override
    public PagedResponse<LeaderboardItemResponse> getCompletedList(UUID assessmentId, Pageable pageable) {
        Page<CandidateAssessment> page = candidateAssessmentRepository
                .findAllByAssessmentIdAndStatusOrderByCompletedAtDesc(assessmentId, CandidateAssessmentStatus.COMPLETED, pageable);
        Map<UUID, ApplicationSummaryDto> applications = fetchApplications(assessmentId);

        return PagedResponse.from(page.map(ca -> {
            ApplicationSummaryDto app = applications.get(ca.getApplicationId());
            return LeaderboardItemResponse.builder()
                    .applicationId(ca.getApplicationId())
                    .candidateName(app != null ? app.getCandidateNameSnapshot() : null)
                    .jobTitle(app != null ? app.getJobTitleSnapshot() : null)
                    .score(ca.getTotalScore()).timeUsedSeconds(ca.getTimeUsedSeconds())
                    .completedAt(ca.getCompletedAt())
                    .build();
        }));
    }

    private String findCandidateNameByScore(UUID assessmentId, Integer score, Map<UUID, ApplicationSummaryDto> applications) {
        if (score == null) {
            return null;
        }
        return candidateAssessmentRepository.findAllByAssessmentIdAndStatusOrderByTotalScoreDescTimeUsedSecondsAsc(assessmentId, CandidateAssessmentStatus.COMPLETED)
                .stream()
                .filter(ca -> score.equals(ca.getTotalScore()))
                .findFirst()
                .map(ca -> applications.get(ca.getApplicationId()))
                .map(ApplicationSummaryDto::getCandidateNameSnapshot)
                .orElse(null);
    }

    private Map<UUID, ApplicationSummaryDto> fetchApplications(UUID assessmentId) {
        UUID jobId = assessmentRepository.findById(assessmentId).map(a -> a.getJobId()).orElse(null);
        if (jobId == null) {
            return Map.of();
        }
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return Map.of();
        }
        return applications.stream().collect(Collectors.toMap(ApplicationSummaryDto::getId, Function.identity(), (a, b) -> a));
    }
}
