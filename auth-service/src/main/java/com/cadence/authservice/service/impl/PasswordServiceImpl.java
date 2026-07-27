package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.entity.PasswordResetToken;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.InvalidCredentialsException;
import com.cadence.authservice.exception.InvalidTokenException;
import com.cadence.authservice.exception.ResourceNotFoundException;
import com.cadence.authservice.exception.TokenExpiredException;
import com.cadence.authservice.kafka.event.PasswordChangedEvent;
import com.cadence.authservice.kafka.event.PasswordResetRequestedEvent;
import com.cadence.authservice.kafka.producer.AuthEventProducer;
import com.cadence.authservice.repository.PasswordResetTokenRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.AuditLogService;
import com.cadence.authservice.service.EmailService;
import com.cadence.authservice.service.PasswordService;
import com.cadence.authservice.service.RefreshTokenService;
import com.cadence.authservice.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuthEventProducer eventProducer;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.security.password-reset-token-expiry-minutes}")
    private long resetTokenExpiryMinutes;

    @Override
    @Transactional
    public void forgotPassword(String email) {
        // Deliberately do not reveal whether the email exists -- returning
        // the same response either way prevents user enumeration attacks.
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = TokenGenerator.generateOpaqueToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(TokenGenerator.sha256(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes))
                    .build();
            passwordResetTokenRepository.save(token);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
            eventProducer.publishPasswordResetRequested(PasswordResetRequestedEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .resetLink("/reset-password?token=" + rawToken)
                    .occurredAt(LocalDateTime.now())
                    .build());
            auditLogService.record(user.getId(), AuditEventType.PASSWORD_RESET_REQUESTED, "Password reset requested", null, null);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenGenerator.sha256(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Password reset token is invalid"));

        if (token.isUsed()) {
            throw new InvalidTokenException("This password reset link has already been used");
        }
        if (token.isExpired()) {
            throw new TokenExpiredException("This password reset link has expired");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        // A password reset invalidates every existing session -- if an
        // attacker had a stolen refresh token, this cuts it off immediately.
        refreshTokenService.revokeAllForUser(user.getId());

        emailService.sendPasswordChangedNotification(user.getEmail(), user.getFullName());
        eventProducer.publishPasswordChanged(PasswordChangedEvent.builder()
                .userId(user.getId()).email(user.getEmail()).occurredAt(LocalDateTime.now()).build());
        auditLogService.record(user.getId(), AuditEventType.PASSWORD_RESET_SUCCESS, "Password reset via token", null, null);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, String ip, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());

        emailService.sendPasswordChangedNotification(user.getEmail(), user.getFullName());
        eventProducer.publishPasswordChanged(PasswordChangedEvent.builder()
                .userId(user.getId()).email(user.getEmail()).occurredAt(LocalDateTime.now()).build());
        auditLogService.record(user.getId(), AuditEventType.PASSWORD_CHANGED, "Password changed by user", ip, userAgent);
    }
}
