package com.cadence.aiinterviewservice.provider;

import com.cadence.aiinterviewservice.exception.InterviewPipelineException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Default provider. Free tier via Google AI Studio (generateContent REST API). */
@Slf4j
@Component
public class GeminiInterviewProvider extends AbstractAIInterviewProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiInterviewProvider(
            @Value("${ai-interview.ai.gemini.base-url}") String baseUrl,
            @Value("${ai-interview.ai.gemini.api-key}") String apiKey,
            @Value("${ai-interview.ai.gemini.model}") String model,
            @Value("${ai-interview.ai.gemini.timeout-seconds:30}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public GeneratedQuestion generateNextQuestion(InterviewQuestionContext context) {
        return extractQuestionJson(callGemini(buildQuestionPrompt(context)));
    }

    @Override
    public InterviewEvaluationData evaluateInterview(InterviewEvaluationContext context) {
        return extractEvaluationJson(callGemini(buildEvaluationPrompt(context)));
    }

    private String callGemini(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new InterviewPipelineException("Gemini API call failed: " + e.getMessage(), e);
        }

        String text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw new InterviewPipelineException("Gemini returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }
}
