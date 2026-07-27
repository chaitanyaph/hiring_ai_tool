package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import com.cadence.codingassessmentservice.dto.response.CodingAnalyticsResponse;
import com.cadence.codingassessmentservice.entity.AssessmentQuestion;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.entity.Question;
import com.cadence.codingassessmentservice.entity.Submission;
import com.cadence.codingassessmentservice.repository.AssessmentQuestionRepository;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.repository.QuestionRepository;
import com.cadence.codingassessmentservice.repository.SubmissionRepository;
import com.cadence.codingassessmentservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Language usage and difficulty success-rate are computed across every
 * submit attempt (not just the latest per question) -- a defensible
 * simplification given a per-assessment candidate pool is small enough
 * that this is cheap, and averaging across attempts is a reasonable
 * proxy for "how candidates actually behaved," not just their final answer.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public CodingAnalyticsResponse getAnalytics(UUID assessmentId) {
        List<CandidateAssessment> candidateAssessments = candidateAssessmentRepository.findAllByAssessmentId(assessmentId);
        long totalInvited = candidateAssessments.size();
        long completed = candidateAssessments.stream().filter(ca -> ca.getStatus() == CandidateAssessmentStatus.COMPLETED).count();
        double completionRate = totalInvited == 0 ? 0.0 : (completed * 100.0) / totalInvited;

        int questionCount = assessmentRepository.findById(assessmentId).map(a -> Math.max(1, a.getQuestionCount())).orElse(1);
        double avgTimeToSolveMinutes = candidateAssessments.stream()
                .filter(ca -> ca.getStatus() == CandidateAssessmentStatus.COMPLETED && ca.getTimeUsedSeconds() != null)
                .mapToDouble(ca -> (ca.getTimeUsedSeconds() / 60.0) / questionCount)
                .average().orElse(0.0);

        List<Submission> allSubmissions = candidateAssessments.stream()
                .flatMap(ca -> submissionRepository.findAllByCandidateAssessmentIdOrderBySubmittedAtAsc(ca.getId()).stream())
                .toList();

        double avgSuccessRate = allSubmissions.isEmpty() ? 0.0
                : (allSubmissions.stream().filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED).count() * 100.0) / allSubmissions.size();

        Map<String, Long> languageCounts = allSubmissions.stream()
                .collect(Collectors.groupingBy(s -> s.getLanguage().name(), Collectors.counting()));
        String mostUsedLanguage = languageCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        List<CodingAnalyticsResponse.LanguageUsage> languageUsage = languageCounts.entrySet().stream()
                .map(e -> CodingAnalyticsResponse.LanguageUsage.builder()
                        .language(e.getKey())
                        .usagePercent(allSubmissions.isEmpty() ? 0.0 : (e.getValue() * 100.0) / allSubmissions.size())
                        .build())
                .toList();

        Map<UUID, Difficulty> questionDifficulty = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(assessmentId).stream()
                .map(AssessmentQuestion::getQuestionId)
                .map(questionRepository::findById)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Question::getId, Question::getDifficulty));

        Map<Difficulty, List<Submission>> byDifficulty = allSubmissions.stream()
                .filter(s -> questionDifficulty.containsKey(s.getQuestionId()))
                .collect(Collectors.groupingBy(s -> questionDifficulty.get(s.getQuestionId())));

        List<CodingAnalyticsResponse.DifficultyBreakdown> difficultyBreakdown = byDifficulty.entrySet().stream()
                .map(e -> CodingAnalyticsResponse.DifficultyBreakdown.builder()
                        .difficulty(e.getKey().name())
                        .successRatePercent(e.getValue().isEmpty() ? 0.0
                                : (e.getValue().stream().filter(s -> s.getStatus() == SubmissionStatus.ACCEPTED).count() * 100.0) / e.getValue().size())
                        .build())
                .toList();

        return CodingAnalyticsResponse.builder()
                .completionRatePercent(completionRate)
                .avgSuccessRatePercent(avgSuccessRate)
                .mostUsedLanguage(mostUsedLanguage)
                .avgTimeToSolveMinutes(avgTimeToSolveMinutes)
                .difficultyBreakdown(difficultyBreakdown)
                .languageUsage(languageUsage)
                .build();
    }
}
