package com.cadence.authservice.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String fullName, String token);
    void sendPasswordResetEmail(String toEmail, String fullName, String token);
    void sendAccountLockedEmail(String toEmail, String fullName);
    void sendPasswordChangedNotification(String toEmail, String fullName);
}
