package com.cadence.aiinterviewservice.provider;

/**
 * Strategy interface -- one implementation per free LLM provider
 * (Gemini/Groq/Ollama). AIInterviewProviderFactory (see the strategy/
 * package) is the context that selects one at runtime from
 * ai-interview.ai.provider in application.yml.
 */
public interface AIInterviewProvider {

    GeneratedQuestion generateNextQuestion(InterviewQuestionContext context);

    InterviewEvaluationData evaluateInterview(InterviewEvaluationContext context);

    String getProviderName();
}
