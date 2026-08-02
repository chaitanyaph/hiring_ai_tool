package com.cadence.authservice.service;

import com.cadence.authservice.dto.request.*;
import com.cadence.authservice.dto.response.AuthResponse;
import com.cadence.authservice.dto.response.TokenResponse;
import com.cadence.authservice.dto.response.UserResponse;
import com.cadence.authservice.entity.User;

import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    /**
     * Finalizes login for an OAuth2-authenticated user (mirroring password login's finalize step),
     * stashes the resulting token/user payload behind a short-lived one-time code, and returns that
     * code -- so the OAuth2 callback (a full-page browser redirect, not an XHR the SPA can read) can
     * hand the browser only an opaque code, which the SPA then redeems via {@link #exchangeOAuthCode}.
     */
    String issueOAuthExchangeCode(User user);

    /** Redeems a one-time code from {@link #issueOAuthExchangeCode} for the token/user payload it was issued for. */
    AuthResponse exchangeOAuthCode(String code);

    AuthResponse completeMfaLogin(MfaVerifyRequest request, String ipAddress, String userAgent);

    TokenResponse refreshToken(String rawRefreshToken, String ipAddress, String userAgent);

    void logout(UUID userId, String rawRefreshToken, boolean allDevices);

    UserResponse getCurrentUser(UUID userId);
}
