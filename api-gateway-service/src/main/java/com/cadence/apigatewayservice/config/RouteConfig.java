package com.cadence.apigatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * The 13 downstream routes, one per business microservice. Path prefix on each route is
 * the service's Eureka application name with its "-service" suffix stripped (job-service
 * is the one irregular case: the Angular frontend was already built against "/jobs",
 * plural, before this Gateway existed -- kept as-is here to match, rather than "fixing"
 * a path the whole frontend already depends on). StripPrefix(1) removes exactly that one
 * path segment so downstream controllers see their own real @RequestMapping("/api/v1/...")
 * unchanged. auth-service is the only public (no-JWT) route; every other route requires a
 * valid JWT via JwtAuthenticationFilter, applied globally in SecurityHeadersGlobalFilter.
 *
 * Every route also gets: a circuit breaker (config in {@link Resilience4jConfig}, falling
 * back to {@link com.cadence.apigatewayservice.controller.FallbackController} when a
 * downstream service is down), a Redis-backed rate limiter (per authenticated user, or
 * per-IP for the public auth routes -- see {@link RateLimiterConfig}), and a retry --
 * deliberately restricted to GET only, since retrying a POST automatically (job
 * application, interview answer, offer action, ...) risks silently duplicating a
 * write the first attempt may have already completed downstream.
 */
@Configuration
public class RouteConfig {

    private static final int RETRY_ATTEMPTS = 2;
    private static final Duration RETRY_FIRST_BACKOFF = Duration.ofMillis(50);
    private static final Duration RETRY_MAX_BACKOFF = Duration.ofMillis(500);

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter, KeyResolver userKeyResolver) {
        return builder.routes()
                .route("auth-service", r -> r.path("/auth/**")
                        .filters(f -> withResilience(f, "auth-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://auth-service"))
                .route("company-service", r -> r.path("/company/**")
                        .filters(f -> withResilience(f, "company-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://company-service"))
                .route("job-service", r -> r.path("/jobs/**")
                        .filters(f -> withResilience(f, "job-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://job-service"))
                .route("candidate-service", r -> r.path("/candidate/**")
                        .filters(f -> withResilience(f, "candidate-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://candidate-service"))
                .route("resume-service", r -> r.path("/resume/**")
                        .filters(f -> withResilience(f, "resume-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://resume-service"))
                .route("application-service", r -> r.path("/application/**")
                        .filters(f -> withResilience(f, "application-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://application-service"))
                .route("resume-parser-service", r -> r.path("/resume-parser/**")
                        .filters(f -> withResilience(f, "resume-parser-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .metadata("response-timeout", 25000)
                        .metadata("connect-timeout", 3000)
                        .uri("lb://resume-parser-service"))
                .route("ai-interview-service", r -> r.path("/ai-interview/**")
                        .filters(f -> withResilience(f, "ai-interview-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .metadata("response-timeout", 25000)
                        .metadata("connect-timeout", 3000)
                        .uri("lb://ai-interview-service"))
                .route("coding-assessment-service", r -> r.path("/coding-assessment/**")
                        .filters(f -> withResilience(f, "coding-assessment-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .metadata("response-timeout", 25000)
                        .metadata("connect-timeout", 3000)
                        .uri("lb://coding-assessment-service"))
                .route("interview-management-service", r -> r.path("/interview-management/**")
                        .filters(f -> withResilience(f, "interview-management-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://interview-management-service"))
                .route("notification-service", r -> r.path("/notification/**")
                        .filters(f -> withResilience(f, "notification-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://notification-service"))
                .route("offer-management-service", r -> r.path("/offer-management/**")
                        .filters(f -> withResilience(f, "offer-management-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://offer-management-service"))
                .route("analytics-service", r -> r.path("/analytics/**")
                        .filters(f -> withResilience(f, "analytics-service", redisRateLimiter, userKeyResolver)
                                .stripPrefix(1))
                        .uri("lb://analytics-service"))
                .build();
    }

    /** Circuit breaker (id "{@code <service>-cb}", falls back to /fallback/{@code <service>}),
     * GET-only retry on 502/503/504, and the shared Redis rate limiter -- applied identically
     * to every route so no route can accidentally be deployed without them. */
    private GatewayFilterSpec withResilience(GatewayFilterSpec f, String serviceId,
                                              RedisRateLimiter redisRateLimiter, KeyResolver userKeyResolver) {
        return f
                .circuitBreaker(c -> c.setName(serviceId + "-cb").setFallbackUri("forward:/fallback/" + serviceId))
                .retry(retryConfig -> retryConfig
                        .setRetries(RETRY_ATTEMPTS)
                        .setMethods(HttpMethod.GET)
                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT)
                        .setExceptions(IOException.class, TimeoutException.class, WebClientResponseException.class)
                        .setBackoff(RETRY_FIRST_BACKOFF, RETRY_MAX_BACKOFF, 2, true))
                .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter).setKeyResolver(userKeyResolver));
    }
}
