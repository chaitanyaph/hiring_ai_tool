package com.cadence.codingassessmentservice.review;

import com.cadence.codingassessmentservice.exception.ExecutionPipelineException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Free tier via Groq's OpenAI-compatible chat completions API -- fast inference on open models. */
@Slf4j
@Component
public class GroqCodeReviewProvider extends AbstractAICodeReviewProvider {

    private final RestClient restClient;
    private final String model;

    public GroqCodeReviewProvider(
            @Value("${coding-assessment.ai.groq.base-url}") String baseUrl,
            @Value("${coding-assessment.ai.groq.api-key}") String apiKey,
            @Value("${coding-assessment.ai.groq.model}") String model,
            @Value("${coding-assessment.ai.groq.timeout-seconds:30}") int timeoutSeconds) {
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public CodeReviewData reviewCode(CodeReviewContext context) {
        return extractReviewJson(callGroq(buildReviewPrompt(context)));
    }

    private String callGroq(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2
        );

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new ExecutionPipelineException("Groq API call failed: " + e.getMessage(), e);
        }

        String text = response.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new ExecutionPipelineException("Groq returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "GROQ";
    }
}
