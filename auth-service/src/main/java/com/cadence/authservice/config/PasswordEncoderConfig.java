package com.cadence.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Split out from SecurityConfig so beans that need only a PasswordEncoder
 * (e.g. AuthServiceImpl) don't pull in SecurityConfig's own dependency
 * graph -- SecurityConfig depends on OAuth2AuthenticationSuccessHandler,
 * which depends on AuthService, which needs a PasswordEncoder; keeping the
 * encoder bean on SecurityConfig itself would make that a circular reference.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12: balances brute-force resistance against login latency.
        return new BCryptPasswordEncoder(12);
    }
}
