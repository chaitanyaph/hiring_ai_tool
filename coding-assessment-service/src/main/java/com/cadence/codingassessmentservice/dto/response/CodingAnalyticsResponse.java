package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.util.List;

/** Backs Tab 4's KPI row + difficulty/language breakdown cards. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingAnalyticsResponse {
    private Double completionRatePercent;
    private Double avgSuccessRatePercent;
    private String mostUsedLanguage;
    private Double avgTimeToSolveMinutes;
    private List<DifficultyBreakdown> difficultyBreakdown;
    private List<LanguageUsage> languageUsage;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DifficultyBreakdown {
        private String difficulty;
        private Double successRatePercent;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LanguageUsage {
        private String language;
        private Double usagePercent;
    }
}
