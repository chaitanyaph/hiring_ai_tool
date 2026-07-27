package com.cadence.codingassessmentservice.execution;

import com.cadence.codingassessmentservice.constants.SubmissionStatus;

/** Reuses SubmissionStatus for the verdict rather than a parallel enum -- the same PENDING/ACCEPTED/WRONG_ANSWER/... vocabulary applies at the execution-engine level and the submission level. */
public record ExecutionResult(
        SubmissionStatus status,
        String stdout,
        String stderr,
        String compileOutput,
        Integer runtimeMs,
        Integer memoryKb
) {}
