package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.entity.EmailVerificationToken;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.InvalidTokenException;
import com.cadence.authservice.exception.ResourceNotFoundException;
import com.cadence.authservice.exception.TokenExpiredException;
import com.cadence.authservice.kafka.event.UserRegisteredEvent;
import com.cadence.authservice.kafka.producer.AuthEventProducer;
import com.cadence.authservice.repository.EmailVerificationTokenRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.AuditLogService;
import com.cadence.authservice.service.EmailService;
import com.cadence.authservice.service.EmailVerificationService;
import com.cadence.authservice.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final AuthEventProducer eventProducer;

    @Value("${app.security.email-verification-token-expiry-hours}")
    private long expiryHours;

    @Override
    @Transactional
    public void sendVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String rawToken = TokenGenerator.generateOpaqueToken();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(TokenGenerator.sha256(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .build();
        tokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), rawToken);

        eventProducer.publishUserRegistered(UserRegisteredEvent.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .userType(user.getUserType().name())
                .companyId(user.getCompanyId())
                .verificationLink("/verify-email?token=" + rawToken)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                sendVerification(user.getId());
            }
        });
        // Same "don't reveal account existence" principle as forgot-password.
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        String hash = TokenGenerator.sha256(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Verification token is invalid"));

        if (token.isUsed()) {
            throw new InvalidTokenException("This verification link has already been used");
        }
        if (token.isExpired()) {
            throw new TokenExpiredException("This verification link has expired. Please request a new one.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        auditLogService.record(user.getId(), AuditEventType.EMAIL_VERIFIED, "Email verified", null, null);
    }
}
