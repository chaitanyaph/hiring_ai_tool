package com.cadence.applicationservice.security;

import com.cadence.applicationservice.constant.SecurityConstants;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            Set<String> permissions = extractStringSet(claims, "permissions");
            String companyIdClaim = claims.get("companyId", String.class);
            String email = claims.get("email", String.class);

            CurrentUser currentUser = CurrentUser.builder()
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(email)
                    .companyId(companyIdClaim != null ? UUID.fromString(companyIdClaim) : null)
                    .role(normalizedRole)
                    .permissions(permissions)
                    .build();

            Collection<GrantedAuthority> authorities = buildAuthorities(normalizedRole, permissions);
            var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private Collection<GrantedAuthority> buildAuthorities(String role, Set<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        if (permissions != null) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        return authorities;
    }

    private String extractSingleRole(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        Object role = claims.get("role");
        return role != null ? String.valueOf(role) : null;
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractStringSet(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return header.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}
