package com.cadence.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a real embedded servlet container (RANDOM_PORT), not @AutoConfigureMockMvc's
 * mock servlet environment: Eureka Server registers its REST endpoints via a Jersey
 * ServletContainer filter, which throws a NullPointerException on FilterRegistration
 * lookup when initialized inside MockMvc's mock ServletContext -- a real container
 * doesn't have that gap.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void actuatorHealth_isPubliclyAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void dashboard_withoutCredentials_isRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void dashboard_withValidCredentials_isAllowed() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("eureka", "eureka_pass")
                .getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
