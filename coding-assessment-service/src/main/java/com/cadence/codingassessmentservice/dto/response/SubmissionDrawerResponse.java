package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

/** Backs the recruiter's openSubmissionDrawer. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDrawerResponse {
    private UUID applicationId;
    private String candidateName;
    private String jobTitle;
    private Integer scorePercent;
    private String testCaseSummary;
    private Integer timeUsedMinutes;
    private String plagiarismBadge;
    private String aiReviewBadge;
    private List<AntiCheatSignal> antiCheatSignals;
    private List<QuestionSubmissionBlock> questions;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AntiCheatSignal {
        private String label;
        private String value;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class QuestionSubmissionBlock {
        private String title;
        private String difficulty;
        private String code;
        private List<TestCaseSummary> testCases;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestCaseSummary {
        private String label;
        private boolean passed;
    }
}
