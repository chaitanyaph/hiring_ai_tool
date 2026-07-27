package com.cadence.authservice.controller;

import com.cadence.authservice.dto.response.ApiResponse;
import com.cadence.authservice.dto.response.SessionResponse;
import com.cadence.authservice.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/sessions")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "View and revoke active device sessions")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all active sessions/devices for the current user")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("OK", sessionService.getActiveSessions(userId, null)));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke a specific session/device")
    public ResponseEntity<ApiResponse<Void>> endSession(@PathVariable UUID sessionId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.endSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok("Session revoked"));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Revoke all sessions/devices (log out everywhere)")
    public ResponseEntity<ApiResponse<Void>> endAllSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        sessionService.endAllSessions(userId);
        return ResponseEntity.ok(ApiResponse.ok("All sessions revoked"));
    }
}
