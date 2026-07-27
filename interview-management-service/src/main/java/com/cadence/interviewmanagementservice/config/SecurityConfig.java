package com.cadence.interviewmanagementservice.config;

import com.cadence.interviewmanagementservice.constants.SecurityConstants;
import com.cadence.interviewmanagementservice.security.JwtAuthenticationFilter;
import com.cadence.interviewmanagementservice.security.RestAccessDeniedHandler;
import com.cadence.interviewmanagementservice.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless: Interview Management Service never issues or stores
 * sessions/tokens, it only validates the JWT Auth Service already
 * issued. Role enforcement is method-level (@PreAuthorize), since
 * access varies by endpoint -- recruiting-role scheduling/decision
 * actions vs. candidate-only read views vs. panelist-scoped feedback
 * submission.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                    .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(restAuthenticationEntryPoint)
                    .accessDeniedHandler(restAccessDeniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
