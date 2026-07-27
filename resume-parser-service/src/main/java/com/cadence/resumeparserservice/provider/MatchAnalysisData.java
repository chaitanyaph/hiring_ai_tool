package com.cadence.resumeparserservice.provider;

import java.util.List;

/** The one fixed contract every provider's analyzeMatch() must produce, mirroring ParsedResumeData's role for parse(). */
public record MatchAnalysisData(
        Integer overallMatchScore,
        Integer technicalSkillScore,
        Integer programmingLanguageScore,
        Integer frameworkScore,
        Integer databaseScore,
        Integer cloudScore,
        Integer devopsScore,
        String experienceMatchLabel,       // STRONG | ADEQUATE | BELOW_RANGE
        Integer projectMatchScore,
        String educationMatchLabel,        // MATCH | PARTIAL | MISMATCH
        String certificationMatchLabel,    // MATCH | PARTIAL | NONE
        Integer communicationPrediction,
        Integer problemSolvingPrediction,
        Integer learningAbilityPrediction,
        List<MatchedSkillItem> matchedSkills,
        List<MissingSkillItem> missingSkills,
        List<String> strengths,
        List<String> weaknesses,
        String recommendedLearningTopics,
        String hiringRecommendation,        // PROCEED | HOLD | REJECT
        String overallAiSummary
) {
    public record MatchedSkillItem(String name, String category) {}

    public record MissingSkillItem(String name, String category, boolean required) {}
}
