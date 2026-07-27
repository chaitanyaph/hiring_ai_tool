package com.cadence.codingassessmentservice.execution;

import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import com.cadence.codingassessmentservice.constants.SubmissionStatus;
import com.cadence.codingassessmentservice.exception.ExecutionPipelineException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Judge0 CE (free, open-source, self-hostable sandboxed execution
 * engine -- https://github.com/judge0/judge0) via its synchronous
 * submission API (wait=true). Never executes candidate code in this
 * service's own JVM/container -- that would be a genuine security
 * hole; Judge0 does the actual isolate-based sandboxing.
 */
@Slf4j
@Component
public class Judge0ExecutionProvider implements CodeExecutionProvider {

    private final RestClient restClient;

    public Judge0ExecutionProvider(
            @Value("${coding-assessment.execution.judge0.base-url}") String baseUrl,
            @Value("${coding-assessment.execution.judge0.api-key:}") String apiKey,
            @Value("${coding-assessment.execution.judge0.timeout-seconds:20}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutSeconds * 1000);
        requestFactory.setReadTimeout(timeoutSeconds * 1000);
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-RapidAPI-Key", apiKey);
        }
        this.restClient = builder.build();
    }

    @Override
    public ExecutionResult execute(ExecutionRequest request) {
        Map<String, Object> requestBody = Map.of(
                "source_code", request.sourceCode(),
                "language_id", languageId(request.language()),
                "stdin", request.stdin() != null ? request.stdin() : "",
                "expected_output", request.expectedOutput() != null ? request.expectedOutput() : "",
                "cpu_time_limit", request.timeLimitSeconds(),
                "memory_limit", request.memoryLimitKb()
        );

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/submissions?base64_encoded=false&wait=true")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new ExecutionPipelineException("Judge0 API call failed: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new ExecutionPipelineException("Judge0 returned an empty response");
        }

        int statusId = response.path("status").path("id").asInt(-1);
        String stdout = textOrNull(response, "stdout");
        String stderr = textOrNull(response, "stderr");
        String compileOutput = textOrNull(response, "compile_output");
        Integer runtimeMs = response.path("time").isMissingNode() || response.path("time").isNull()
                ? null : (int) Math.round(response.path("time").asDouble() * 1000);
        Integer memoryKb = response.path("memory").isMissingNode() || response.path("memory").isNull()
                ? null : response.path("memory").asInt();

        return new ExecutionResult(toSubmissionStatus(statusId), stdout, stderr, compileOutput, runtimeMs, memoryKb);
    }

    private String textOrNull(JsonNode response, String field) {
        JsonNode node = response.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    /** Judge0's own status.id vocabulary -- https://ce.judge0.com/#statuses-and-languages-status-get */
    private SubmissionStatus toSubmissionStatus(int statusId) {
        return switch (statusId) {
            case 3 -> SubmissionStatus.ACCEPTED;
            case 4 -> SubmissionStatus.WRONG_ANSWER;
            case 5 -> SubmissionStatus.TIME_LIMIT_EXCEEDED;
            case 6 -> SubmissionStatus.COMPILE_ERROR;
            case 7, 8, 9, 10, 11, 12 -> SubmissionStatus.RUNTIME_ERROR;
            default -> SubmissionStatus.RUNTIME_ERROR;
        };
    }

    /** Judge0 CE's well-known language_id values. */
    private int languageId(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> 62;
            case PYTHON -> 71;
            case JAVASCRIPT -> 63;
            case CPP -> 54;
            case SQL -> 82;
        };
    }

    @Override
    public String getProviderName() {
        return "JUDGE0";
    }
}
