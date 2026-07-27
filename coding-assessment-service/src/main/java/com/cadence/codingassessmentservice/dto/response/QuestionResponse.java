package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.Difficulty;
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
    private Difficulty difficulty;
    private int marks;
    private String description;
    private String exampleText;
    private String constraintsText;
    private List<String> tags;
    private List<String> allowedLanguages;
    private Map<String, String> starterCodes;
    private List<TestCaseResponse> testCases;
    private int hiddenTestCaseCount;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestCaseResponse {
        private UUID id;
        private String visibility;
        private String inputData;
        private String expectedOutput;
    }
}
