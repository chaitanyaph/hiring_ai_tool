package com.cadence.resumeparserservice.constants;

/**
 * Free-LLM providers supported via the Strategy pattern (see the
 * provider/ and strategy/ packages). Selected purely by
 * resume-parser.ai.provider in application.yml -- switching providers
 * never requires a code change.
 */
public enum AiProvider {
    GEMINI,
    GROQ,
    OLLAMA
}
