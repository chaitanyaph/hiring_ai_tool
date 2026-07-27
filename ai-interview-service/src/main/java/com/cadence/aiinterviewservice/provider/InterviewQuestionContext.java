package com.cadence.aiinterviewservice.provider;

import com.cadence.aiinterviewservice.constants.QuestionCategory;

import java.util.List;

public record InterviewQuestionContext(
        CandidateResumeSnapshot resume,
        JobContextSnapshot job,
        QuestionCategory category,
        int questionNumber,
        int totalQuestions,
        List<QaPair> priorQuestionsAndAnswers
) {}
