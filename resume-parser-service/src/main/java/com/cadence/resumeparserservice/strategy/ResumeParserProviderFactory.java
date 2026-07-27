package com.cadence.resumeparserservice.strategy;

import com.cadence.resumeparserservice.provider.ResumeParserProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The Strategy *context*: collects every ResumeParserProvider bean
 * Spring knows about and selects one by name at runtime from
 * resume-parser.ai.provider. Switching providers is a config change
 * only -- this class never needs to change when a new provider is
 * added, as long as it's a Spring-managed ResumeParserProvider bean.
 */
@Slf4j
@Component
public class ResumeParserProviderFactory {

    private final Map<String, ResumeParserProvider> providersByName;
    private final String activeProviderName;

    public ResumeParserProviderFactory(List<ResumeParserProvider> providers,
                                        @Value("${resume-parser.ai.provider}") String activeProviderName) {
        this.providersByName = providers.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
        this.activeProviderName = activeProviderName.toUpperCase();
    }

    @PostConstruct
    void logActiveProvider() {
        log.info("Resume Parser Service active AI provider: {}", activeProviderName);
    }

    public ResumeParserProvider getActiveProvider() {
        ResumeParserProvider provider = providersByName.get(activeProviderName);
        if (provider == null) {
            throw new IllegalStateException("Unknown resume-parser.ai.provider '" + activeProviderName
                    + "' -- expected one of " + providersByName.keySet());
        }
        return provider;
    }
}
