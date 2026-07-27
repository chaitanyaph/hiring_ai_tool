package com.cadence.jobservice.client;

import com.cadence.jobservice.client.dto.FeignApiResponse;
import com.cadence.jobservice.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Interface only, per architecture: Auth Service doesn't yet expose a
 * "get any user by id" endpoint (only "get my own profile"). This is
 * scaffolding for showing the recruiter/hiring manager's name instead
 * of a bare UUID once that endpoint exists -- role/permission checks
 * themselves never need this call, they come straight off the JWT.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{userId}")
    FeignApiResponse<UserDto> getUserById(@PathVariable("userId") UUID userId);
}
