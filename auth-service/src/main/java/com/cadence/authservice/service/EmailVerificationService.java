package com.cadence.authservice.service;

import java.util.UUID;

public interface EmailVerificationService {
    void sendVerification(UUID userId);
    void resendVerification(String email);
    void verify(String rawToken);
}
