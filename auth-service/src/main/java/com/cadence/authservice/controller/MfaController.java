package com.cadence.authservice.controller;

import com.cadence.authservice.dto.request.MfaVerifyRequest;
import com.cadence.authservice.dto.response.ApiResponse;
import com.cadence.authservice.dto.response.MfaSetupResponse;
import com.cadence.authservice.service.AuthService;
import com.cadence.authservice.service.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * MFA enrollment endpoints (as opposed to the login-time challenge,
 * which lives on AuthController /mfa/verify-login since it happens
 * before the user has an access token).
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "Multi-Factor Authentication", description = "TOTP-based MFA setup and management")
public class MfaController {

    private final MfaService mfaService;
    private final AuthService authService;

    @PostMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Begin MFA enrollment: returns a TOTP secret, QR code, and recovery codes")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setup(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        String email = authService.getCurrentUser(userId).getEmail();
        MfaSetupResponse response = mfaService.setupMfa(userId, email);
        return ResponseEntity.ok(ApiResponse.ok("Scan the QR code with your authenticator app, then confirm with a code", response));
    }

    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm MFA enrollment by submitting a valid 6-digit code")
    public ResponseEntity<ApiResponse<Void>> confirm(@Valid @RequestBody MfaVerifyRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        mfaService.confirmMfaSetup(userId, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("MFA has been enabled for your account"));
    }

    @PostMapping("/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Disable MFA (requires current password for confirmation)")
    public ResponseEntity<ApiResponse<Void>> disable(@RequestParam String currentPassword, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        mfaService.disableMfa(userId, currentPassword);
        return ResponseEntity.ok(ApiResponse.ok("MFA has been disabled for your account"));
    }
}
