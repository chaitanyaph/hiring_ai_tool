package com.cadence.codingassessmentservice.dto.response;

import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Backs the per-case pass/fail pills and Input/Your Output/Expected Output breakdown after Submit. Hidden test cases show pass/fail only, never their input/expected output. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitCodeResponse {
    private UUID submissionId;
    private SubmissionStatus status;
    private Integer score;
    private Integer testCasesPassed;
    private Integer testCasesTotal;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String compileOutput;
    private List<TestCaseResult> testCaseResults;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestCaseResult {
        private boolean visible;
        private boolean passed;
        private String inputData;
        private String expectedOutput;
        private String actualOutput;
    }
}
