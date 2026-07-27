package com.cadence.applicationservice.client;

import com.cadence.applicationservice.client.dto.FeignApiResponse;
import com.cadence.applicationservice.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Interface only, per architecture: Auth Service doesn't yet expose a
 * "get any user by id" endpoint, and JWT validation itself is done
 * locally (shared HS256 secret), never via a live call to Auth Service
 * -- calling out synchronously on every request would make this
 * service's availability depend on Auth Service's uptime, which the
 * whole platform deliberately avoids. This is scaffolding for a future
 * need (e.g. resolving a recruiter's display name), not called anywhere yet.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{userId}")
    FeignApiResponse<UserDto> getUserById(@PathVariable("userId") UUID userId);
}
