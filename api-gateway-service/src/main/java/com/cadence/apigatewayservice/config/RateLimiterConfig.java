package com.cadence.apigatewayservice.config;

import com.cadence.apigatewayservice.constant.SecurityConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Backs every route's rate limiter against the same Upstash Redis instance every
 * service already uses for caching -- no extra infrastructure. Keyed by the verified
 * X-User-Id header {@link com.cadence.apigatewayservice.security.JwtAuthenticationGlobalFilter}
 * sets on authenticated requests, so limits are per-user rather than per-connection;
 * requests with no user yet (the public auth routes -- login, register, ...) fall back
 * to the client IP, which also gives basic brute-force protection on those routes.
 */
@Configuration
public class RateLimiterConfig {

    /** 20 requests/sec sustained, burst up to 40 -- generous for normal UI usage
     * (dashboards firing a handful of parallel calls per navigation) while still
     * bounding a runaway client or scripted abuse. */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(SecurityConstants.USER_ID_HEADER);
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
