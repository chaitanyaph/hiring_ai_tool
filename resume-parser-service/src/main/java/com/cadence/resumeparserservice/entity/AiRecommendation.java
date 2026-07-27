package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.HiringRecommendation;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "ai_recommendation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "resume_match_id", nullable = false, unique = true)
    private UUID resumeMatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_recommendation", nullable = false, length = 20)
    private HiringRecommendation hiringRecommendation;

    @Column(name = "overall_ai_summary", columnDefinition = "TEXT")
    private String overallAiSummary;

    // Plain narrative text, no dedicated Figma section or endpoint --
    // same judgment call as "achievements" in the parsing extension.
    @Column(name = "recommended_learning_topics", columnDefinition = "TEXT")
    private String recommendedLearningTopics;
}
