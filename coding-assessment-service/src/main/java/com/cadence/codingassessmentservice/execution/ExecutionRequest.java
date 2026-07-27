package com.cadence.codingassessmentservice.execution;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;

/** The one fixed contract every execution provider's execute() accepts -- used for both unscored "Run" (expectedOutput null) and scored "Submit" (one call per test case). */
public record ExecutionRequest(
        ProgrammingLanguage language,
        String sourceCode,
        String stdin,
        String expectedOutput,
        int timeLimitSeconds,
        int memoryLimitKb
) {}
