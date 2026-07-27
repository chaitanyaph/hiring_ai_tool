package com.cadence.authservice.service;

public interface PasswordService {
    void forgotPassword(String email);
    void resetPassword(String rawToken, String newPassword);
    void changePassword(java.util.UUID userId, String currentPassword, String newPassword, String ip, String userAgent);
}
