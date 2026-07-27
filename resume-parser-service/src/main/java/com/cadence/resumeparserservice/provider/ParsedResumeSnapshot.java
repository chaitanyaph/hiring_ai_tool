package com.cadence.resumeparserservice.provider;

import java.math.BigDecimal;
import java.util.List;

/** The candidate-side input to analyzeMatch -- a lean projection of this service's own parsed_resume + child tables, not a persistence entity. */
public record ParsedResumeSnapshot(
        String fullName,
        String professionalSummary,
        List<String> skills,
        BigDecimal totalExperienceYears,
        List<String> experienceSummaries,
        List<String> educationSummaries,
        List<String> certificationNames,
        List<String> projectSummaries
) {}
