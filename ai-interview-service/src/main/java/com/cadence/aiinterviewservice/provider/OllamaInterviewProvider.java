package com.cadence.aiinterviewservice.provider;

import com.cadence.aiinterviewservice.exception.InterviewPipelineException;
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
public class OllamaInterviewProvider extends AbstractAIInterviewProvider {

    private final RestClient restClient;
    private final String model;

    public OllamaInterviewProvider(
            @Value("${ai-interview.ai.ollama.base-url}") String baseUrl,
            @Value("${ai-interview.ai.ollama.model}") String model,
            @Value("${ai-interview.ai.ollama.timeout-seconds:60}") int timeoutSeconds) {
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public GeneratedQuestion generateNextQuestion(InterviewQuestionContext context) {
        return extractQuestionJson(callOllama(buildQuestionPrompt(context)));
    }

    @Override
    public InterviewEvaluationData evaluateInterview(InterviewEvaluationContext context) {
        return extractEvaluationJson(callOllama(buildEvaluationPrompt(context)));
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
            throw new InterviewPipelineException("Ollama call failed: " + e.getMessage(), e);
        }

        String text = response.path("response").asText(null);
        if (text == null || text.isBlank()) {
            throw new InterviewPipelineException("Ollama returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "OLLAMA";
    }
}
