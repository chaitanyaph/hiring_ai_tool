package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.NoteType;
import com.cadence.codingassessmentservice.entity.*;
import com.cadence.codingassessmentservice.feign.ApplicationServiceClient;
import com.cadence.codingassessmentservice.feign.dto.ScoreUpdateRequest;
import com.cadence.codingassessmentservice.kafka.event.CodingAssessmentCompletedEvent;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.repository.*;
import com.cadence.codingassessmentservice.review.CodeReviewContext;
import com.cadence.codingassessmentservice.review.CodeReviewData;
import com.cadence.codingassessmentservice.service.CodingEvaluationService;
import com.cadence.codingassessmentservice.strategy.AICodeReviewProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The async evaluation pipeline: AI code review per question's latest
 * submission, aggregate the final score, write back to Application
 * Service, publish CodingAssessmentCompleted. Triggered by
 * CandidateAssessmentServiceImpl.finishAssessment() -- @Async is safe
 * here without a separate pipeline-runner bean because it's always
 * invoked from a *different* bean, same reasoning ai-interview-
 * service's InterviewEvaluationServiceImpl already documents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodingEvaluationServiceImpl implements CodingEvaluationService {

    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubmissionRepository submissionRepository;
    private final AiCodeReviewRepository aiCodeReviewRepository;
    private final AiCodeReviewNoteRepository aiCodeReviewNoteRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final AICodeReviewProviderFactory providerFactory;
    private final CodingAssessmentEventProducer eventProducer;

    @Override
    @Async
    @Transactional
    public void evaluate(UUID candidateAssessmentId) {
        Optional<CandidateAssessment> maybeCa = candidateAssessmentRepository.findById(candidateAssessmentId);
        if (maybeCa.isEmpty()) {
            return;
        }
        CandidateAssessment ca = maybeCa.get();
        try {
            Assessment assessment = assessmentRepository.findById(ca.getAssessmentId()).orElseThrow();
            List<AssessmentQuestion> links = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(ca.getAssessmentId());

            int totalScore = 0;
            int testCasesPassed = 0;
            int testCasesTotal = 0;

            for (AssessmentQuestion link : links) {
                Optional<Submission> latest = submissionRepository
                        .findFirstByCandidateAssessmentIdAndQuestionIdOrderBySubmittedAtDesc(ca.getId(), link.getQuestionId());
                if (latest.isEmpty()) {
                    continue;
                }
                Submission submission = latest.get();
                totalScore += submission.getScore() != null ? submission.getScore() : 0;
                testCasesPassed += submission.getTestCasesPassed() != null ? submission.getTestCasesPassed() : 0;
                testCasesTotal += submission.getTestCasesTotal() != null ? submission.getTestCasesTotal() : 0;

                reviewSubmission(submission);
            }

            ca.setTotalScore(totalScore);
            ca.setTestCasesPassed(testCasesPassed);
            ca.setTestCasesTotal(testCasesTotal);
            candidateAssessmentRepository.save(ca);

            int scorePercent = assessment.getTotalMarks() == 0 ? 0
                    : Math.round((float) totalScore * 100 / assessment.getTotalMarks());

            applicationServiceClient.updateCodingScore(ca.getApplicationId(),
                    ScoreUpdateRequest.builder().score(scorePercent).source("coding-assessment-service").build());

            eventProducer.publishCodingAssessmentCompleted(CodingAssessmentCompletedEvent.builder()
                    .applicationId(ca.getApplicationId()).score(scorePercent).build());

            log.info("Evaluation completed for candidate assessment {} -- score {}%", candidateAssessmentId, scorePercent);
        } catch (Exception e) {
            log.warn("Evaluation failed for candidate assessment {}: {}", candidateAssessmentId, e.getMessage(), e);
        }
    }

    private void reviewSubmission(Submission submission) {
        Question question = questionRepository.findById(submission.getQuestionId()).orElse(null);
        CodeReviewContext context = new CodeReviewContext(
                question != null ? question.getTitle() : null,
                question != null ? question.getDescription() : null,
                submission.getLanguage().name(),
                submission.getCode(),
                submission.getTestCasesPassed(),
                submission.getTestCasesTotal());

        CodeReviewData data = providerFactory.getActiveProvider().reviewCode(context);

        AiCodeReview review = aiCodeReviewRepository.findBySubmissionId(submission.getId())
                .orElseGet(() -> AiCodeReview.builder().submissionId(submission.getId()).build());
        review.setTimeComplexity(data.timeComplexity());
        review.setSpaceComplexity(data.spaceComplexity());
        review.setNamingConventionNotes(data.namingConventionNotes());
        review.setCodeQualityScore(data.codeQualityScore());
        review.setSolidPrinciplesNotes(data.solidPrinciplesNotes());
        review.setDesignPatternsNotes(data.designPatternsNotes());
        review.setSecurityIssues(data.securityIssues());
        review.setOptimizationSuggestions(data.optimizationSuggestions());
        review.setCleanCodeNotes(data.cleanCodeNotes());
        review.setOverallRating(data.overallRating());
        review = aiCodeReviewRepository.save(review);

        // Re-evaluation replaces notes wholesale -- same "recalculate fully replaces children" precedent used across the platform.
        aiCodeReviewNoteRepository.deleteAllByAiCodeReviewId(review.getId());
        List<AiCodeReviewNote> notes = new ArrayList<>();
        appendNotes(notes, review.getId(), NoteType.STRENGTH, data.strengths());
        appendNotes(notes, review.getId(), NoteType.WEAKNESS, data.weaknesses());
        appendNotes(notes, review.getId(), NoteType.SUGGESTION, data.suggestions());
        aiCodeReviewNoteRepository.saveAll(notes);
    }

    private void appendNotes(List<AiCodeReviewNote> notes, UUID reviewId, NoteType noteType, List<String> descriptions) {
        if (descriptions == null) {
            return;
        }
        int order = 0;
        for (String description : descriptions) {
            notes.add(AiCodeReviewNote.builder().aiCodeReviewId(reviewId).noteType(noteType).description(description).displayOrder(order++).build());
        }
    }
}
