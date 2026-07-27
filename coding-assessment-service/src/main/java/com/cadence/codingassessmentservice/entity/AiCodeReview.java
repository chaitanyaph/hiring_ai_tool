package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.AiProvider;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** 1:1 with submission. The Figma's submission drawer only visualizes a simplified "Clean / Needs attention" badge derived from overallRating -- this table holds the full structured review your Module 6 spec explicitly asked for. */
@Entity
@Table(name = "ai_code_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCodeReview {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "submission_id", nullable = false, unique = true)
    private UUID submissionId;

    @Column(name = "time_complexity", length = 50)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 50)
    private String spaceComplexity;

    @Column(name = "naming_convention_notes", columnDefinition = "TEXT")
    private String namingConventionNotes;

    @Column(name = "code_quality_score")
    private Integer codeQualityScore;

    @Column(name = "solid_principles_notes", columnDefinition = "TEXT")
    private String solidPrinciplesNotes;

    @Column(name = "design_patterns_notes", columnDefinition = "TEXT")
    private String designPatternsNotes;

    @Column(name = "security_issues", columnDefinition = "TEXT")
    private String securityIssues;

    @Column(name = "optimization_suggestions", columnDefinition = "TEXT")
    private String optimizationSuggestions;

    @Column(name = "clean_code_notes", columnDefinition = "TEXT")
    private String cleanCodeNotes;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_used", length = 20)
    private AiProvider providerUsed;
}
