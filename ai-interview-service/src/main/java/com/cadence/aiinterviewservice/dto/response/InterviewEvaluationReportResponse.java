package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** The full aggregate backing the "AI interview evaluation report" drawer. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewEvaluationReportResponse {
    private UUID applicationId;
    private UUID candidateId;
    private String fullName;
    private UUID jobId;
    private String jobTitle;

    private Integer overallScore;
    private Integer communicationScore;
    private Integer confidenceScore;
    private Integer technicalAccuracyScore;
    private Integer problemSolvingScore;
    private Integer grammarScore;
    private Integer behaviorScore;
    private Integer leadershipScore;
    private Integer domainKnowledgeScore;

    private Integer eyeContactScore;
    private Integer speakingPaceScore;
    private Integer fillerWordCount;
    private Integer avgResponseLatencySeconds;

    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> improvementAreas;
    private HiringRecommendation hiringRecommendation;
    private String interviewSummary;
    private String recruiterSummary;

    private List<TranscriptTurnResponse> transcript;
    private LocalDateTime completedAt;
}
