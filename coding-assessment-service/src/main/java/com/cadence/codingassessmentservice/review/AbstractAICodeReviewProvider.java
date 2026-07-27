package com.cadence.codingassessmentservice.review;

import com.cadence.codingassessmentservice.exception.ExecutionPipelineException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared prompt template + JSON-extraction logic for every provider -- same "LLMs sometimes wrap JSON in markdown fences" fix factored out once, mirrors the platform's other two AI provider layers. */
public abstract class AbstractAICodeReviewProvider implements AICodeReviewProvider {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String REVIEW_PROMPT_TEMPLATE = """
            You are a senior software engineer performing a code review for a hiring platform's coding
            assessment. Review the submitted solution below and return ONLY a single valid JSON object
            matching this exact schema -- no markdown, no explanation, no code fences, just the raw JSON object.

            {
              "timeComplexity": string (e.g. "O(n log n)"),
              "spaceComplexity": string (e.g. "O(n)"),
              "namingConventionNotes": string,
              "codeQualityScore": integer 0-100,
              "solidPrinciplesNotes": string,
              "designPatternsNotes": string,
              "securityIssues": string (or "None identified"),
              "optimizationSuggestions": string,
              "cleanCodeNotes": string,
              "overallRating": integer 0-100,
              "strengths": [string, ...],
              "weaknesses": [string, ...],
              "suggestions": [string, ...]
            }

            Rules:
            - Base every assessment strictly on the code below -- do not invent behavior not present.
            - Return ONLY the JSON object, nothing else.

            Question: %s
            Description: %s

            Language: %s
            Test cases passed: %s of %s

            Submitted code:
            \"\"\"
            %s
            \"\"\"
            """;

    protected final String buildReviewPrompt(CodeReviewContext context) {
        return REVIEW_PROMPT_TEMPLATE.formatted(
                nullToNone(context.questionTitle()), nullToNone(context.questionDescription()),
                nullToNone(context.language()), nullToNone(context.testCasesPassed()), nullToNone(context.testCasesTotal()),
                context.code());
    }

    private String nullToNone(Object value) {
        return value == null ? "Not specified" : String.valueOf(value);
    }

    protected final CodeReviewData extractReviewJson(String rawOutput) {
        String cleaned = stripMarkdownFences(rawOutput);
        try {
            return OBJECT_MAPPER.readValue(cleaned, CodeReviewData.class);
        } catch (Exception e) {
            throw new ExecutionPipelineException(
                    "Provider " + getProviderName() + " returned invalid JSON: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
