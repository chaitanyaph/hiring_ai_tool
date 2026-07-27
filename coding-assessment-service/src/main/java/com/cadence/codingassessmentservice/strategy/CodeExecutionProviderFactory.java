package com.cadence.codingassessmentservice.strategy;

import com.cadence.codingassessmentservice.execution.CodeExecutionProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Strategy *context*: collects every CodeExecutionProvider bean
 * Spring knows about and selects one by name at runtime from
 * coding-assessment.execution.provider. Only Judge0 exists today, but
 * kept as a real factory (not a direct @Autowired Judge0ExecutionProvider)
 * so a second sandbox backend later is a config change, not a rewrite.
 */
@Slf4j
@Component
public class CodeExecutionProviderFactory {

    private final Map<String, CodeExecutionProvider> providersByName;
    private final String activeProviderName;

    public CodeExecutionProviderFactory(List<CodeExecutionProvider> providers,
                                         @Value("${coding-assessment.execution.provider}") String activeProviderName) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
        this.activeProviderName = activeProviderName.toUpperCase();
    }

    @PostConstruct
    void logActiveProvider() {
        log.info("Coding Assessment Service active code execution provider: {}", activeProviderName);
    }

    public CodeExecutionProvider getActiveProvider() {
        CodeExecutionProvider provider = providersByName.get(activeProviderName);
        if (provider == null) {
            throw new IllegalStateException("Unknown coding-assessment.execution.provider '" + activeProviderName
                    + "' -- expected one of " + providersByName.keySet());
        }
        return provider;
    }
}
