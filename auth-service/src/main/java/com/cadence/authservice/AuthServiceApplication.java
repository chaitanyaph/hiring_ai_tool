package com.cadence.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Cadence Authentication & Authorization microservice.
 *
 * Registers with Eureka (service discovery) and pulls shared configuration
 * from the Config Server, per the platform's Spring Cloud microservices
 * architecture (see HLD - Volume 3).
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
        System.out.println("Lets Start Developing the Auth Service Application.....");
    }
}
