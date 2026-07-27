package com.cadence.aiinterviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "interview_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewScore {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "communication_score")
    private Integer communicationScore;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "technical_accuracy_score")
    private Integer technicalAccuracyScore;

    @Column(name = "problem_solving_score")
    private Integer problemSolvingScore;

    @Column(name = "grammar_score")
    private Integer grammarScore;

    @Column(name = "behavior_score")
    private Integer behaviorScore;

    @Column(name = "leadership_score")
    private Integer leadershipScore;

    @Column(name = "domain_knowledge_score")
    private Integer domainKnowledgeScore;

    @Column(name = "overall_score")
    private Integer overallScore;

    // Behaviour analysis metrics -- fixed, typed columns rather than a
    // generic label/value table, same fixed-schema philosophy as every
    // other score table in this platform.
    @Column(name = "eye_contact_score")
    private Integer eyeContactScore;

    @Column(name = "speaking_pace_score")
    private Integer speakingPaceScore;

    @Column(name = "filler_word_count")
    private Integer fillerWordCount;

    @Column(name = "avg_response_latency_seconds")
    private Integer avgResponseLatencySeconds;
}
