package com.cadence.authservice.service;

import com.cadence.authservice.dto.request.*;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    AuthResponse completeMfaLogin(MfaVerifyRequest request, String ipAddress, String userAgent);

    TokenResponse refreshToken(String rawRefreshToken, String ipAddress, String userAgent);

    void logout(UUID userId, String rawRefreshToken, boolean allDevices);

    UserResponse getCurrentUser(UUID userId);
}
