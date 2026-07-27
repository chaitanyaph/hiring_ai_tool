package com.cadence.authservice.controller;

import com.cadence.authservice.dto.request.*;
import com.cadence.authservice.dto.response.ApiResponse;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.security.CustomUserDetails;
import com.cadence.authservice.service.AuthService;
import com.cadence.authservice.service.EmailVerificationService;
import com.cadence.authservice.service.PasswordService;
import com.cadence.authservice.util.RequestMetadataUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, password & email flows")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user (candidate, recruiter, or company admin)")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. Please check your email to verify your account.", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email/password. Returns tokens directly, or an mfaSessionToken if MFA is enabled.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request,
                RequestMetadataUtil.extractClientIp(httpRequest), RequestMetadataUtil.extractUserAgent(httpRequest));
        String message = response.isMfaRequired() ? "MFA verification required" : "Login successful";
        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }

    @PostMapping("/mfa/verify-login")
    @Operation(summary = "Complete login by submitting the 6-digit MFA code for the pending session")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyMfaLogin(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.completeMfaLogin(request,
                RequestMetadataUtil.extractClientIp(httpRequest), RequestMetadataUtil.extractUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Exchange a valid refresh token for a new access/refresh token pair (rotation)")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken(),
                RequestMetadataUtil.extractClientIp(httpRequest), RequestMetadataUtil.extractUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Log out the current session, or all devices if allDevices=true")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        authService.logout(userId, request.getRefreshToken(), request.isAllDevices());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link via email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If an account exists with that email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token emailed by /forgot-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password has been reset successfully. Please log in with your new password."));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password while logged in (requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                             Authentication authentication, HttpServletRequest httpRequest) {
        UUID userId = UUID.fromString(authentication.getName());
        passwordService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword(),
                RequestMetadataUtil.extractClientIp(httpRequest), RequestMetadataUtil.extractUserAgent(httpRequest));
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address using the token emailed at registration")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email verification link")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("If an unverified account exists with that email, a new verification link has been sent."));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("OK", authService.getCurrentUser(userId)));
    }
}
