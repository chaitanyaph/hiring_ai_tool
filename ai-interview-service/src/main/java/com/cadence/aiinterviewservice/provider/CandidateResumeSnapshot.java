package com.cadence.aiinterviewservice.provider;

import java.util.List;

/** The candidate-side input to both question generation and evaluation -- a lean projection of resume-parser-service's ResumeMatchResponse, not a persistence entity. */
public record CandidateResumeSnapshot(
        String fullName,
        String professionalSummary,
        List<String> skills,
        List<String> experienceSummaries
) {}
