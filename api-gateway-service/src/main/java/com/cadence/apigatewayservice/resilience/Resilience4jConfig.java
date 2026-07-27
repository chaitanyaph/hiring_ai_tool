package com.cadence.apigatewayservice.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * One default circuit-breaker/time-limiter profile for the 10 routes that hit a normal
 * CRUD backend, and a separate, more lenient profile for the 3 routes that call out to
 * an LLM (resume-parser-service, ai-interview-service, coding-assessment-service) --
 * Gemini/Groq calls routinely take longer than a typical DB-backed request, so those
 * three need a longer timeout and a more tolerant failure threshold or they'd trip
 * the breaker on ordinary LLM latency, not on an actual outage. Circuit-breaker ids
 * here ("<service>-cb") must match the ids RouteConfig assigns per route.
 */
@Configuration
public class Resilience4jConfig {

    private static final List<String> AI_HEAVY_SERVICES = List.of(
            "resume-parser-service", "ai-interview-service", "coding-assessment-service");

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(8))
                .build();

        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig)
                .build());
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> aiHeavyCircuitBreakerCustomizer() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(25))
                .build();

        return factory -> AI_HEAVY_SERVICES.forEach(service -> factory.configure(builder -> builder
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig), service + "-cb"));
    }
}
