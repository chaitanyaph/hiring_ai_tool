package com.cadence.codingassessmentservice.dto.response;

import lombok.*;

import java.util.List;

/** The full structured AI code review your Module 6 spec asked for -- the Figma's submission drawer only surfaces a derived "Clean/Needs attention" badge from overallRating, but the richer data is fully modeled and available here. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCodeReviewResponse {
    private String timeComplexity;
    private String spaceComplexity;
    private String namingConventionNotes;
    private Integer codeQualityScore;
    private String solidPrinciplesNotes;
    private String designPatternsNotes;
    private String securityIssues;
    private String optimizationSuggestions;
    private String cleanCodeNotes;
    private Integer overallRating;
    private String badge; // "Clean" | "Needs attention" -- derived from overallRating
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
}
