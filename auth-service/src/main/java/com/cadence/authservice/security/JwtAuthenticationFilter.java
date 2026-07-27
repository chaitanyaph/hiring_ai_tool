package com.cadence.authservice.security;

import com.cadence.authservice.constant.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates the access token on every request and populates the
 * SecurityContext. Also checks a Redis blacklist so tokens can be
 * revoked immediately on logout, even though JWTs are otherwise
 * stateless -- this closes the "can't revoke a JWT before it expires"
 * gap without forcing us back to full server-side sessions.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            if (isBlacklisted(token)) {
                writeUnauthorized(response, "Token has been revoked. Please log in again.");
                return;
            }

            if (jwtTokenProvider.isTokenValid(token)) {
                Claims claims = jwtTokenProvider.parseClaims(token);
                Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);

                var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        List<String> roles = safeList(claims.get("roles"));
        List<String> permissions = safeList(claims.get("permissions"));
        return java.util.stream.Stream.concat(roles.stream(), permissions.stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> safeList(Object claim) {
        if (claim instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey("auth:blacklist:" + token);
        return Boolean.TRUE.equals(exists);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", message
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
