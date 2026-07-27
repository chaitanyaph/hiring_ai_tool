package com.cadence.eurekaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * An unauthenticated Eureka server on a shared network lets anyone browse the
 * dashboard or register/deregister instances of any of the 14 client services --
 * a real production risk, not a demo nicety. Basic auth is required everywhere,
 * including /eureka/** (client register/renew/deregister calls) -- every client's
 * defaultZone URL carries the same credentials embedded (http://user:pass@host/eureka),
 * which Spring Cloud Netflix's Eureka client supports natively. CSRF is disabled
 * only for /eureka/**, since Eureka clients never send a CSRF token; the dashboard
 * itself (browser-driven) keeps CSRF protection.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {});
        return http.build();
    }
}
