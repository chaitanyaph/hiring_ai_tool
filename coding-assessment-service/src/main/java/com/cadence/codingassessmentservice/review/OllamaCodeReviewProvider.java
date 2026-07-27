package com.cadence.codingassessmentservice.review;

import com.cadence.codingassessmentservice.exception.ExecutionPipelineException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Local, zero-cost provider via a locally running Ollama instance -- no API key required. */
@Slf4j
@Component
public class OllamaCodeReviewProvider extends AbstractAICodeReviewProvider {

    private final RestClient restClient;
    private final String model;

    public OllamaCodeReviewProvider(
            @Value("${coding-assessment.ai.ollama.base-url}") String baseUrl,
            @Value("${coding-assessment.ai.ollama.model}") String model,
            @Value("${coding-assessment.ai.ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public CodeReviewData reviewCode(CodeReviewContext context) {
        return extractReviewJson(callOllama(buildReviewPrompt(context)));
    }

    private String callOllama(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/api/generate")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new ExecutionPipelineException("Ollama call failed: " + e.getMessage(), e);
        }

        String text = response.path("response").asText(null);
        if (text == null || text.isBlank()) {
            throw new ExecutionPipelineException("Ollama returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "OLLAMA";
    }
}
