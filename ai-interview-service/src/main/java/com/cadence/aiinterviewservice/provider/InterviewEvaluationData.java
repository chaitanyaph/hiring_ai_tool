package com.cadence.aiinterviewservice.provider;

import java.util.List;

/**
 * The one fixed contract every provider's evaluateInterview() must produce.
 * Deliberately excludes eyeContactScore/speakingPaceScore/fillerWordCount/
 * avgResponseLatencySeconds -- there's no real audio/video capture pipeline
 * on this platform (only server-side TTS output and client-side text
 * transcription), so an LLM asked for those would just be fabricating
 * plausible-looking numbers with zero basis in any actual observation.
 * fillerWordCount and avgResponseLatencySeconds ARE derivable from real data
 * (the transcript text and each answer's stored responseTimeSeconds) and are
 * computed deterministically in InterviewEvaluationServiceImpl instead; eye
 * contact and speaking pace have no substitute real signal and are left null.
 */
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
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvementAreas,
        String hiringRecommendation,   // PROCEED | HOLD | REJECT
        String interviewSummary,
        String recruiterSummary
) {}
