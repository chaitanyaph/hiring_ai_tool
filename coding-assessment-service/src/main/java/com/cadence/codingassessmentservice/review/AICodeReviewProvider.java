package com.cadence.codingassessmentservice.review;

/**
 * Strategy interface -- one implementation per free LLM provider
 * (Gemini/Groq/Ollama). AICodeReviewProviderFactory (see the
 * strategy/ package) is the context that selects one at runtime from
 * coding-assessment.ai.provider in application.yml.
 */
public interface AICodeReviewProvider {

    CodeReviewData reviewCode(CodeReviewContext context);

    String getProviderName();
}
