package com.cadence.resumeparserservice.provider;

import java.util.List;

/** The job-side input to analyzeMatch -- a lean projection of Job Service's JobDetailResponse fetched via Feign, not a persistence entity. */
public record JobRequirementsSnapshot(
        String jobTitle,
        Integer minExperienceYears,
        Integer maxExperienceYears,
        List<String> requiredSkills,
        List<String> preferredSkills,
        String education,
        String certifications
) {}
