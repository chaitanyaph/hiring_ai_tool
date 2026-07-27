package com.cadence.jobservice.constant;

import java.util.List;

public final class PipelineStageDefaults {
    private PipelineStageDefaults() {}

    public static final List<String> DEFAULT_STAGES = List.of(
            "Application Received",
            "Resume Screening",
            "AI Resume Screening",
            "AI Interview",
            "Coding Assessment",
            "Technical Interview",
            "Hiring Manager Interview",
            "HR Interview",
            "Offer",
            "Background Verification",
            "Hired"
    );
}
