package com.cadence.aiinterviewservice.provider;

import java.util.List;

/** The job-side input to both question generation and evaluation -- a lean projection of Job Service's JobDetailResponse fetched via Feign. */
public record JobContextSnapshot(
        String jobTitle,
        List<String> requiredSkills
) {}
