package com.cadence.aiinterviewservice.provider;

import com.cadence.aiinterviewservice.exception.InterviewPipelineException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Shared prompt template + JSON-extraction logic for every provider --
 * factored out so the "LLMs sometimes wrap JSON in markdown fences"
 * problem is solved once, not three times. Mirrors Resume Parser
 * Service's AbstractResumeParserProvider pattern.
 */
public abstract class AbstractAIInterviewProvider implements AIInterviewProvider {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String QUESTION_PROMPT_TEMPLATE = """
            You are conducting a live AI job interview. Generate ONE interview question for the category below,
            tailored to the specific candidate and job -- return ONLY a single valid JSON object matching this
            exact schema -- no markdown, no explanation, no code fences, just the raw JSON object.

            {
              "questionText": string
            }

            Rules:
            - The question must belong to category: %s
            - This is question %s of %s in the interview.
            - Do not repeat or closely paraphrase any of the previously asked questions below.
            - Keep the question concise (1-3 sentences), natural spoken-interview phrasing, no numbering/prefix.

            Job: %s
            Required skills: %s

            Candidate: %s
            Professional summary: %s
            Skills: %s
            Experience: %s

            Previously asked questions and answers in this interview:
            %s
            """;

    private static final String EVALUATION_PROMPT_TEMPLATE = """
            You are an AI interview evaluator for a hiring platform. Evaluate the complete interview transcript
            below against the candidate's resume and the job's requirements, and return ONLY a single valid JSON
            object matching this exact schema -- no markdown, no explanation, no code fences, just the raw JSON object.

            {
              "communicationScore": integer 0-100,
              "confidenceScore": integer 0-100,
              "technicalAccuracyScore": integer 0-100,
              "problemSolvingScore": integer 0-100,
              "grammarScore": integer 0-100,
              "behaviorScore": integer 0-100,
              "leadershipScore": integer 0-100,
              "domainKnowledgeScore": integer 0-100,
              "overallScore": integer 0-100,
              "strengths": [string, ...],
              "weaknesses": [string, ...],
              "improvementAreas": [string, ...],
              "hiringRecommendation": one of PROCEED, HOLD, REJECT,
              "interviewSummary": string (2-4 sentences, candidate-facing tone),
              "recruiterSummary": string (2-4 sentences, recruiter-facing tone, more direct)
            }

            Rules:
            - Base every score strictly on the transcript content below -- do not invent facts not present.
            - Return ONLY the JSON object, nothing else.

            Job: %s
            Required skills: %s

            Candidate: %s
            Professional summary: %s

            Interview transcript:
            %s
            """;

    protected final String buildQuestionPrompt(InterviewQuestionContext context) {
        return QUESTION_PROMPT_TEMPLATE.formatted(
                context.category(), context.questionNumber(), context.totalQuestions(),
                nullToNone(context.job().jobTitle()), joinOrNone(context.job().requiredSkills()),
                nullToNone(context.resume().fullName()), nullToNone(context.resume().professionalSummary()),
                joinOrNone(context.resume().skills()), joinOrNone(context.resume().experienceSummaries()),
                joinQaOrNone(context.priorQuestionsAndAnswers()));
    }

    protected final String buildEvaluationPrompt(InterviewEvaluationContext context) {
        return EVALUATION_PROMPT_TEMPLATE.formatted(
                nullToNone(context.job().jobTitle()), joinOrNone(context.job().requiredSkills()),
                nullToNone(context.resume().fullName()), nullToNone(context.resume().professionalSummary()),
                joinTranscriptOrNone(context.transcript()));
    }

    private String nullToNone(Object value) {
        return value == null ? "Not specified" : String.valueOf(value);
    }

    private String joinOrNone(List<String> values) {
        return (values == null || values.isEmpty()) ? "None listed" : String.join("; ", values);
    }

    private String joinQaOrNone(List<QaPair> qa) {
        if (qa == null || qa.isEmpty()) {
            return "None yet -- this is the first question.";
        }
        StringBuilder sb = new StringBuilder();
        for (QaPair pair : qa) {
            sb.append("Q: ").append(pair.questionText()).append("\nA: ").append(pair.answerText()).append("\n");
        }
        return sb.toString();
    }

    private String joinTranscriptOrNone(List<TranscriptTurn> transcript) {
        if (transcript == null || transcript.isEmpty()) {
            return "Empty transcript.";
        }
        StringBuilder sb = new StringBuilder();
        for (TranscriptTurn turn : transcript) {
            sb.append(turn.speaker()).append(": ").append(turn.text()).append("\n");
        }
        return sb.toString();
    }

    protected final GeneratedQuestion extractQuestionJson(String rawOutput) {
        return extractJson(rawOutput, GeneratedQuestion.class);
    }

    protected final InterviewEvaluationData extractEvaluationJson(String rawOutput) {
        return extractJson(rawOutput, InterviewEvaluationData.class);
    }

    private <T> T extractJson(String rawOutput, Class<T> type) {
        String cleaned = stripMarkdownFences(rawOutput);
        try {
            return OBJECT_MAPPER.readValue(cleaned, type);
        } catch (Exception e) {
            throw new InterviewPipelineException(
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
