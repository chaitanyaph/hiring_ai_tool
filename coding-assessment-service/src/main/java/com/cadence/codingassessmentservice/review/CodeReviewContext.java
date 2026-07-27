package com.cadence.codingassessmentservice.review;

public record CodeReviewContext(
        String questionTitle,
        String questionDescription,
        String language,
        String code,
        Integer testCasesPassed,
        Integer testCasesTotal
) {}
