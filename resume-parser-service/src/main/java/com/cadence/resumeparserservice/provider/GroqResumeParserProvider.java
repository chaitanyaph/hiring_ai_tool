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

/** Free tier via Groq's OpenAI-compatible chat completions API -- fast inference on open models. */
@Slf4j
@Component
public class GroqResumeParserProvider extends AbstractResumeParserProvider {

    private final RestClient restClient;
    private final String model;

    public GroqResumeParserProvider(
            @Value("${resume-parser.ai.groq.base-url}") String baseUrl,
            @Value("${resume-parser.ai.groq.api-key}") String apiKey,
            @Value("${resume-parser.ai.groq.model}") String model,
            @Value("${resume-parser.ai.groq.timeout-seconds:30}") int timeoutSeconds) {
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
    public ParsedResumeData parse(String resumeText) {
        return extractJson(callGroq(buildPrompt(resumeText)));
    }

    @Override
    public MatchAnalysisData analyzeMatch(ParsedResumeSnapshot resume, JobRequirementsSnapshot job) {
        return extractMatchJson(callGroq(buildMatchPrompt(resume, job)));
    }

    private String callGroq(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.1
        );

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new ResumeParsingPipelineException("Groq API call failed: " + e.getMessage(), e);
        }

        String text = response.path("choices").path(0).path("message").path("content").asText(null);
        if (text == null || text.isBlank()) {
            throw new ResumeParsingPipelineException("Groq returned an empty response");
        }
        return text;
    }

    @Override
    public String getProviderName() {
        return "GROQ";
    }
}
