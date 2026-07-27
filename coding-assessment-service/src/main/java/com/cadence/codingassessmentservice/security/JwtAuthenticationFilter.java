package com.cadence.codingassessmentservice.security;

import com.cadence.codingassessmentservice.constants.SecurityConstants;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Validates the access token issued by Auth Service and populates the
 * SecurityContext with a CurrentUser principal. Roles arrive from Auth
 * Service either as "ROLE_X" or bare "X" -- normalized here so
 * hasRole() checks work regardless of that prefix convention drifting
 * between services over time.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenValidator.isTokenValid(token)) {
            Claims claims = jwtTokenValidator.parseClaims(token);

            String rawRole = extractSingleRole(claims);
            String normalizedRole = rawRole == null ? null : rawRole.replaceFirst("^ROLE_", "");
            String companyIdClaim = claims.get("companyId", String.class);
            String email = claims.get("email", String.class);

            CurrentUser currentUser = CurrentUser.builder()
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(email)
                    .companyId(companyIdClaim != null ? UUID.fromString(companyIdClaim) : null)
                    .role(normalizedRole)
                    .build();

            Collection<GrantedAuthority> authorities = normalizedRole == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
            var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractSingleRole(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        Object role = claims.get("role");
        return role != null ? String.valueOf(role) : null;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}
