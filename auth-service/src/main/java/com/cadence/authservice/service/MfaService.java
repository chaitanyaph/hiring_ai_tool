package com.cadence.authservice.service;

import com.cadence.authservice.dto.response.MfaSetupResponse;

import java.util.UUID;

public interface MfaService {
    MfaSetupResponse setupMfa(UUID userId, String email);
    void confirmMfaSetup(UUID userId, String code);
    void disableMfa(UUID userId, String currentPassword);
    boolean verifyCode(UUID userId, String code);
    String issueMfaSessionToken(UUID userId);
    UUID resolveMfaSessionToken(String mfaSessionToken);
}
