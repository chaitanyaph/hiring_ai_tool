package com.cadence.resumeparserservice.provider;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
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
public class GeminiResumeParserProvider extends AbstractResumeParserProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiResumeParserProvider(
            @Value("${resume-parser.ai.gemini.base-url}") String baseUrl,
            @Value("${resume-parser.ai.gemini.api-key}") String apiKey,
            @Value("${resume-parser.ai.gemini.model}") String model,
            @Value("${resume-parser.ai.gemini.timeout-seconds:30}") int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public ParsedResumeData parse(String resumeText) {
        return extractJson(callGemini(buildPrompt(resumeText)));
    }

    @Override
    public MatchAnalysisData analyzeMatch(ParsedResumeSnapshot resume, JobRequirementsSnapshot job) {
        return extractMatchJson(callGemini(buildMatchPrompt(resume, job)));
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
            throw new ResumeParsingPipelineException("Gemini API call failed: " + e.getMessage(), e);
        }

        String text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
        if (text == null || text.isBlank()) {
            throw new ResumeParsingPipelineException("Gemini returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }
}
