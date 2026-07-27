package com.cadence.codingassessmentservice.review;

import java.util.List;

/** The one fixed contract every provider's reviewCode() must produce -- covers every field Module 6 of the spec asked for. */
public record CodeReviewData(
        String timeComplexity,
        String spaceComplexity,
        String namingConventionNotes,
        Integer codeQualityScore,
        String solidPrinciplesNotes,
        String designPatternsNotes,
        String securityIssues,
        String optimizationSuggestions,
        String cleanCodeNotes,
        Integer overallRating,
        List<String> strengths,
        List<String> weaknesses,
        List<String> suggestions
) {}
