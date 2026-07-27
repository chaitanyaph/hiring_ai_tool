package com.cadence.interviewmanagementservice.entity;

import com.cadence.interviewmanagementservice.constants.RecommendationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Merges the suggested feedback_score table into this one row.
 * communicationScore/technicalScore/cultureFitScore are the 3
 * dimensions the Figma's submit-feedback modal actually collects;
 * codingSkillsScore/problemSolvingScore/systemDesignScore/
 * leadershipScore are supported per the text spec's fuller Module 5
 * model but are nullable -- not reachable from the current Figma form.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "interview_feedback")
public class InterviewFeedback {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "technical_score")
    private Integer technicalScore;

    @Column(name = "culture_fit_score")
    private Integer cultureFitScore;

    @Column(name = "coding_skills_score")
    private Integer codingSkillsScore;

    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    @Column(name = "system_design_score")
    private Integer systemDesignScore;

    @Column(name = "leadership_score")
    private Integer leadershipScore;

    @Column(name = "overall_rating")
    private Integer overallRating;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", nullable = false, length = 20)
    private RecommendationType recommendation;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}
