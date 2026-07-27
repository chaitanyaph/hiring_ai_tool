package com.cadence.resumeparserservice.provider;

/**
 * Strategy interface -- one implementation per free LLM provider
 * (Gemini/Groq/Ollama). ResumeParserProviderFactory (see the
 * strategy/ package) is the context that selects one at runtime from
 * resume-parser.ai.provider in application.yml.
 */
public interface ResumeParserProvider {

    ParsedResumeData parse(String resumeText);

    /**
     * Second capability on the same providers rather than a parallel
     * ResumeAIProvider/GeminiResumeProvider hierarchy -- Gemini/Groq/
     * Ollama each already have the HTTP client + config wiring parse()
     * needs; duplicating that for a second interface would just be the
     * same three REST clients built twice.
     */
    MatchAnalysisData analyzeMatch(ParsedResumeSnapshot resume, JobRequirementsSnapshot job);

    String getProviderName();
}
