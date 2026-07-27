package com.cadence.candidateservice.client;

import com.cadence.candidateservice.client.dto.FeignApiResponse;
import com.cadence.candidateservice.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Interface only, per architecture: Auth Service doesn't yet expose a
 * "get any user by id" endpoint (only "get my own profile"), and the
 * candidate's own full name/email are captured directly onto the
 * Candidate row at profile-creation time instead. This is scaffolding
 * for a future need (e.g. an admin support view), not called anywhere
 * in the current codebase.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/users/{userId}")
    FeignApiResponse<UserDto> getUserById(@PathVariable("userId") UUID userId);
}
