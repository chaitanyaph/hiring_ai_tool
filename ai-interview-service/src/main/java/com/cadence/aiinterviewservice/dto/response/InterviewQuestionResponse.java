package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.QuestionCategory;
import lombok.*;

import java.util.UUID;

/** Candidate-facing: one question returned from start/answer, backing the live interview screen. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestionResponse {
    private UUID questionId;
    private int questionOrder;
    private int totalQuestions;
    private QuestionCategory category;
    private String questionText;
    private boolean interviewCompleted;
}
