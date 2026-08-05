package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.Difficulty;
import com.cadence.codingassessmentservice.constants.QuestionStatus;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Recruiter-facing question bank view -- includes hidden test cases (candidate-facing IdeQuestionResponse strips those out). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private UUID id;
    private String title;
    private QuestionStatus status;
    private Difficulty difficulty;
    private int marks;
    private String description;
    private String exampleText;
    private String constraintsText;
    private String inputFormat;
    private String outputFormat;
    private String explanation;
    private List<String> tags;
    private List<String> topics;
    private List<String> hints;
    private int timeLimitMs;
    private int memoryLimitMb;
    private List<String> allowedLanguages;
    private Map<String, String> starterCodes;
    private List<TestCaseResponse> testCases;
    /** Populated on both the list and detail endpoints -- unlike testCases (detail-only), this is cheap to compute in bulk and is what the bank's list table renders. */
    private int testCaseCount;
    private int hiddenTestCaseCount;
    private int usedInAssessmentCount;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestCaseResponse {
        private UUID id;
        private String visibility;
        private String inputData;
        private String expectedOutput;
        private String explanation;
        private int weight;
        private int displayOrder;
    }
}
