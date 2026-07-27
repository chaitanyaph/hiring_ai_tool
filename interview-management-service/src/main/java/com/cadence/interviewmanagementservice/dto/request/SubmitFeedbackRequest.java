package com.cadence.interviewmanagementservice.dto.request;

import com.cadence.interviewmanagementservice.constants.RecommendationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Matches modal-submit-feedback (§A4) exactly: Communication/Technical/
 * Culture fit (1-10), Strengths, Concerns/weaknesses, Recommendation.
 * The remaining 4 dimensions are optional/nullable -- supported per the
 * text spec's fuller Module 5 model but not reachable from the current
 * Figma form.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitFeedbackRequest {

    @NotNull @Min(1) @Max(10)
    private Integer communicationScore;

    @NotNull @Min(1) @Max(10)
    private Integer technicalScore;

    @NotNull @Min(1) @Max(10)
    private Integer cultureFitScore;

    @Min(1) @Max(10)
    private Integer codingSkillsScore;

    @Min(1) @Max(10)
    private Integer problemSolvingScore;

    @Min(1) @Max(10)
    private Integer systemDesignScore;

    @Min(1) @Max(10)
    private Integer leadershipScore;

    @Min(1) @Max(10)
    private Integer overallRating;

    private String strengths;

    private String weaknesses;

    private String comments;

    @NotNull
    private RecommendationType recommendation;
}
