package com.cadence.codingassessmentservice.strategy;

import com.cadence.codingassessmentservice.review.AICodeReviewProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Strategy *context*: collects every AICodeReviewProvider bean
 * Spring knows about and selects one by name at runtime from
 * coding-assessment.ai.provider. Switching providers is a config
 * change only.
 */
@Slf4j
@Component
public class AICodeReviewProviderFactory {

    private final Map<String, AICodeReviewProvider> providersByName;
    private final String activeProviderName;

    public AICodeReviewProviderFactory(List<AICodeReviewProvider> providers,
                                        @Value("${coding-assessment.ai.provider}") String activeProviderName) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
        this.activeProviderName = activeProviderName.toUpperCase();
    }

    @PostConstruct
    void logActiveProvider() {
        log.info("Coding Assessment Service active AI code review provider: {}", activeProviderName);
    }

    public AICodeReviewProvider getActiveProvider() {
        AICodeReviewProvider provider = providersByName.get(activeProviderName);
        if (provider == null) {
            throw new IllegalStateException("Unknown coding-assessment.ai.provider '" + activeProviderName
                    + "' -- expected one of " + providersByName.keySet());
        }
        return provider;
    }
}
