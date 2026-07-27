package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.Difficulty;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Candidate-facing IDE question view -- only visible test cases, only the starter code for the candidate's selected language. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdeQuestionResponse {
    private UUID questionId;
    private int questionOrder;
    private int totalQuestions;
    private Difficulty difficulty;
    private int marks;
    private int hiddenTestCaseCount;
    private String title;
    private String description;
    private String exampleText;
    private String constraintsText;
    private List<String> allowedLanguages;
    private String starterCode;
    private List<VisibleTestCase> visibleTestCases;
    private String navigatorStatus; // NOT_VISITED | VISITED | COMPLETED
    private boolean markedForReview;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VisibleTestCase {
        private String inputData;
        private String expectedOutput;
    }
}
