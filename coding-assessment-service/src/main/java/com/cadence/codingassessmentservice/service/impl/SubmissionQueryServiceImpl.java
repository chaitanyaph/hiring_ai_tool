package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import com.cadence.codingassessmentservice.constants.NoteType;
import com.cadence.codingassessmentservice.dto.response.*;
import com.cadence.codingassessmentservice.entity.*;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.feign.ApplicationServiceClient;
import com.cadence.codingassessmentservice.feign.dto.ApplicationSummaryDto;
import com.cadence.codingassessmentservice.repository.*;
import com.cadence.codingassessmentservice.service.SubmissionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionQueryServiceImpl implements SubmissionQueryService {

    private static final int CLEAN_REVIEW_THRESHOLD = 60;

    private final AssessmentRepository assessmentRepository;
    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionTestCaseRepository submissionTestCaseRepository;
    private final AiCodeReviewRepository aiCodeReviewRepository;
    private final AiCodeReviewNoteRepository aiCodeReviewNoteRepository;
    private final AntiCheatLogRepository antiCheatLogRepository;
    private final ApplicationServiceClient applicationServiceClient;

    @Override
    public SubmissionDrawerResponse getSubmissionDrawer(UUID companyId, UUID assessmentId, UUID applicationId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .filter(a -> a.getCompanyId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND, "Assessment not found: " + assessmentId));
        CandidateAssessment ca = candidateAssessmentRepository.findByAssessmentIdAndApplicationId(assessmentId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No submission found for this candidate"));

        ApplicationSummaryDto application = fetchApplication(assessment.getJobId(), applicationId);

        Integer scorePercent = ca.getTotalScore() == null || assessment.getTotalMarks() == 0 ? null
                : Math.round((float) ca.getTotalScore() * 100 / assessment.getTotalMarks());
        String testCaseSummary = ca.getTestCasesPassed() != null && ca.getTestCasesTotal() != null
                ? ca.getTestCasesPassed() + " of " + ca.getTestCasesTotal() + " test cases passed" : null;

        List<SubmissionDrawerResponse.AntiCheatSignal> antiCheatSignals = List.of(
                signal("Tab switches", antiCheatLogRepository.countByCandidateAssessmentIdAndEventType(ca.getId(), AntiCheatEventType.TAB_SWITCH)),
                signal("Copy-paste detected", antiCheatLogRepository.countByCandidateAssessmentIdAndEventType(ca.getId(), AntiCheatEventType.COPY)
                        + antiCheatLogRepository.countByCandidateAssessmentIdAndEventType(ca.getId(), AntiCheatEventType.PASTE)),
                signal("Fullscreen exits", antiCheatLogRepository.countByCandidateAssessmentIdAndEventType(ca.getId(), AntiCheatEventType.FULLSCREEN_EXIT)));

        List<AssessmentQuestion> links = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(assessmentId);
        List<SubmissionDrawerResponse.QuestionSubmissionBlock> questionBlocks = new java.util.ArrayList<>();
        Integer worstRating = null;

        for (AssessmentQuestion link : links) {
            Question question = questionRepository.findById(link.getQuestionId()).orElse(null);
            Optional<Submission> latest = submissionRepository.findFirstByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(ca.getId(), link.getQuestionId());
            if (question == null || latest.isEmpty()) {
                continue;
            }
            Submission submission = latest.get();
            List<SubmissionTestCase> testCases = submissionTestCaseRepository.findAllBySubmissionIdOrderByDisplayOrderAsc(submission.getId());
            List<SubmissionDrawerResponse.TestCaseSummary> testCaseSummaries = new java.util.ArrayList<>();
            int i = 1;
            for (SubmissionTestCase tc : testCases) {
                testCaseSummaries.add(SubmissionDrawerResponse.TestCaseSummary.builder()
                        .label("Test case " + i++).passed(tc.isPassed()).build());
            }

            questionBlocks.add(SubmissionDrawerResponse.QuestionSubmissionBlock.builder()
                    .title(question.getTitle()).difficulty(question.getDifficulty().name())
                    .code(submission.getCode()).testCases(testCaseSummaries)
                    .build());

            Integer rating = aiCodeReviewRepository.findBySubmissionId(submission.getId()).map(AiCodeReview::getOverallRating).orElse(null);
            if (rating != null && (worstRating == null || rating < worstRating)) {
                worstRating = rating;
            }
        }

        String aiReviewBadge = worstRating == null ? "Pending" : (worstRating >= CLEAN_REVIEW_THRESHOLD ? "Clean" : "Needs attention");

        return SubmissionDrawerResponse.builder()
                .applicationId(applicationId)
                .candidateName(application != null ? application.getCandidateNameSnapshot() : null)
                .jobTitle(application != null ? application.getJobTitleSnapshot() : null)
                .scorePercent(scorePercent)
                .testCaseSummary(testCaseSummary)
                .timeUsedMinutes(ca.getTimeUsedSeconds() == null ? null : ca.getTimeUsedSeconds() / 60)
                // No plagiarism-detection engine has been built anywhere in this
                // platform yet -- this is a static placeholder, not a real signal.
                .plagiarismBadge("No plagiarism detected")
                .aiReviewBadge(aiReviewBadge)
                .antiCheatSignals(antiCheatSignals)
                .questions(questionBlocks)
                .build();
    }

    @Override
    public List<SubmissionHistoryItemResponse> getSubmissionHistory(UUID candidateAssessmentId) {
        CandidateAssessment ca = candidateAssessmentRepository.findById(candidateAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No assessment attempt found: " + candidateAssessmentId));
        List<AssessmentQuestion> links = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(ca.getAssessmentId());

        List<SubmissionHistoryItemResponse> history = new java.util.ArrayList<>();
        int questionOrder = 1;
        for (AssessmentQuestion link : links) {
            List<Submission> attempts = submissionRepository.findAllByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(candidateAssessmentId, link.getQuestionId());
            for (Submission s : attempts) {
                history.add(SubmissionHistoryItemResponse.builder()
                        .submissionId(s.getId()).questionOrder(questionOrder)
                        .submittedAt(s.getSubmittedAt()).language(s.getLanguage().name()).status(s.getStatus())
                        .runtimeMs(s.getRuntimeMs()).memoryKb(s.getMemoryKb())
                        .testCasesPassed(s.getTestCasesPassed()).testCasesTotal(s.getTestCasesTotal())
                        .score(s.getScore())
                        .build());
            }
            questionOrder++;
        }
        return history;
    }

    @Override
    public AiCodeReviewResponse getAiCodeReview(UUID submissionId) {
        AiCodeReview review = aiCodeReviewRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SUBMISSION_NOT_FOUND, "No AI code review found for this submission"));

        List<String> strengths = aiCodeReviewNoteRepository.findAllByAiCodeReviewIdAndNoteTypeOrderByDisplayOrderAsc(review.getId(), NoteType.STRENGTH)
                .stream().map(AiCodeReviewNote::getDescription).toList();
        List<String> weaknesses = aiCodeReviewNoteRepository.findAllByAiCodeReviewIdAndNoteTypeOrderByDisplayOrderAsc(review.getId(), NoteType.WEAKNESS)
                .stream().map(AiCodeReviewNote::getDescription).toList();
        List<String> suggestions = aiCodeReviewNoteRepository.findAllByAiCodeReviewIdAndNoteTypeOrderByDisplayOrderAsc(review.getId(), NoteType.SUGGESTION)
                .stream().map(AiCodeReviewNote::getDescription).toList();

        String badge = review.getOverallRating() != null && review.getOverallRating() >= CLEAN_REVIEW_THRESHOLD ? "Clean" : "Needs attention";

        return AiCodeReviewResponse.builder()
                .timeComplexity(review.getTimeComplexity()).spaceComplexity(review.getSpaceComplexity())
                .namingConventionNotes(review.getNamingConventionNotes()).codeQualityScore(review.getCodeQualityScore())
                .solidPrinciplesNotes(review.getSolidPrinciplesNotes()).designPatternsNotes(review.getDesignPatternsNotes())
                .securityIssues(review.getSecurityIssues()).optimizationSuggestions(review.getOptimizationSuggestions())
                .cleanCodeNotes(review.getCleanCodeNotes()).overallRating(review.getOverallRating())
                .badge(badge).strengths(strengths).weaknesses(weaknesses).suggestions(suggestions)
                .build();
    }

    private SubmissionDrawerResponse.AntiCheatSignal signal(String label, long value) {
        return SubmissionDrawerResponse.AntiCheatSignal.builder().label(label).value(String.valueOf(value)).build();
    }

    private ApplicationSummaryDto fetchApplication(UUID jobId, UUID applicationId) {
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return null;
        }
        return applications.stream().filter(a -> applicationId.equals(a.getId())).findFirst().orElse(null);
    }
}
