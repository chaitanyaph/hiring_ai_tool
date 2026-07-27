package com.cadence.companyservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Interface only, per architecture: Auth Service owns accounts/login,
 * this service never creates or reads credentials. Scaffolding for a
 * future "is this user still active" lookup if a team-management screen
 * needs it -- unused by any business logic in this module today.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{userId}")
    Map<String, Object> getUserById(@PathVariable("userId") UUID userId);
}
