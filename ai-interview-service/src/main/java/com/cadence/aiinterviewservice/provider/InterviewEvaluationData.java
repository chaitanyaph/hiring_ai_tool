package com.cadence.aiinterviewservice.provider;

import java.util.List;

/** The one fixed contract every provider's evaluateInterview() must produce. */
public record InterviewEvaluationData(
        Integer communicationScore,
        Integer confidenceScore,
        Integer technicalAccuracyScore,
        Integer problemSolvingScore,
        Integer grammarScore,
        Integer behaviorScore,
        Integer leadershipScore,
        Integer domainKnowledgeScore,
        Integer overallScore,
        Integer eyeContactScore,
        Integer speakingPaceScore,
        Integer fillerWordCount,
        Integer avgResponseLatencySeconds,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvementAreas,
        String hiringRecommendation,   // PROCEED | HOLD | REJECT
        String interviewSummary,
        String recruiterSummary
) {}
