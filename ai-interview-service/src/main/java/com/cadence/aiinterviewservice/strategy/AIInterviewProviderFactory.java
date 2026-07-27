package com.cadence.aiinterviewservice.strategy;

import com.cadence.aiinterviewservice.provider.AIInterviewProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Strategy *context*: collects every AIInterviewProvider bean
 * Spring knows about and selects one by name at runtime from
 * ai-interview.ai.provider. Switching providers is a config change
 * only -- this class never needs to change when a new provider is
 * added, as long as it's a Spring-managed AIInterviewProvider bean.
 */
@Slf4j
@Component
public class AIInterviewProviderFactory {

    private final Map<String, AIInterviewProvider> providersByName;
    private final String activeProviderName;

    public AIInterviewProviderFactory(List<AIInterviewProvider> providers,
                                       @Value("${ai-interview.ai.provider}") String activeProviderName) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
        this.activeProviderName = activeProviderName.toUpperCase();
    }

    @PostConstruct
    void logActiveProvider() {
        log.info("AI Interview Service active AI provider: {}", activeProviderName);
    }

    public AIInterviewProvider getActiveProvider() {
        AIInterviewProvider provider = providersByName.get(activeProviderName);
        if (provider == null) {
            throw new IllegalStateException("Unknown ai-interview.ai.provider '" + activeProviderName
                    + "' -- expected one of " + providersByName.keySet());
        }
        return provider;
    }
}
