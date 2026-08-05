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
    /** Base64 MP3 (Google Cloud TTS), null when synthesis is disabled/unavailable -- the frontend falls back to text-only silently. */
    private String audioBase64;
    private boolean interviewCompleted;
}
