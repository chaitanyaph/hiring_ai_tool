package com.cadence.aiinterviewservice.provider;

import java.util.List;

public record InterviewEvaluationContext(
        CandidateResumeSnapshot resume,
        JobContextSnapshot job,
        List<TranscriptTurn> transcript
) {}
