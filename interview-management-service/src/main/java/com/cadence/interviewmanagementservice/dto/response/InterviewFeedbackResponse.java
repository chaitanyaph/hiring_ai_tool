package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.RecommendationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Backs #drawer-interview-feedback-view (§A5). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedbackResponse {
    private UUID id;
    private UUID interviewId;
    private UUID interviewerId;
    private String interviewerName;
    private Integer communicationScore;
    private Integer technicalScore;
    private Integer cultureFitScore;
    private Integer codingSkillsScore;
    private Integer problemSolvingScore;
    private Integer systemDesignScore;
    private Integer leadershipScore;
    private Integer overallRating;
    private String strengths;
    private String weaknesses;
    private String comments;
    private RecommendationType recommendation;
    private LocalDateTime submittedAt;
}
