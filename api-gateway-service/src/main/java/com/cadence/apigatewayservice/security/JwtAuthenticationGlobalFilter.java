package com.cadence.apigatewayservice.security;

import com.cadence.apigatewayservice.constant.SecurityConstants;
import com.cadence.apigatewayservice.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Perimeter authentication: every request through this Gateway either matches a public
 * path (registration/login/refresh/etc.) or must carry a valid JWT. This does NOT
 * replace each downstream service's own JwtAuthenticationFilter -- those stay as
 * defense-in-depth and are the ones that actually build a SecurityContext for
 * @PreAuthorize checks. What this filter adds: (1) requests with a missing/expired/
 * tampered token are rejected here, before ever reaching a downstream service or its
 * database connection pool; (2) X-User-Id/X-Company-Id/X-User-Role are stripped from
 * whatever the client sent and re-set here from the verified token's claims, so
 * company-service's currently-trusted-but-unauthenticated X-User-Id header (see
 * platform docs) becomes safe in practice once this Gateway is the only path in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final JwtSupport jwtSupport;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());
        if (token == null || !jwtSupport.isValid(token)) {
            return reject(exchange, "Missing or invalid access token");
        }

        Claims claims = jwtSupport.parseClaims(token);
        String userId = claims.getSubject();
        String companyId = claims.get("companyId", String.class);
        String role = jwtSupport.primaryRole(claims);

        ServerHttpRequest.Builder mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(SecurityConstants.USER_ID_HEADER);
                    headers.remove(SecurityConstants.COMPANY_ID_HEADER);
                    headers.remove(SecurityConstants.USER_ROLE_HEADER);
                    if (userId != null) headers.set(SecurityConstants.USER_ID_HEADER, userId);
                    if (companyId != null) headers.set(SecurityConstants.COMPANY_ID_HEADER, companyId);
                    if (role != null) headers.set(SecurityConstants.USER_ROLE_HEADER, role);
                });

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    private boolean isPublic(String path) {
        return SecurityConstants.PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(ApiResponse.error(message));
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Failed to serialize 401 body", e);
            byte[] fallback = ("{\"success\":false,\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
