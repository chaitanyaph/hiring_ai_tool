package com.cadence.authservice.service;

import com.cadence.authservice.entity.PasswordResetToken;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.InvalidTokenException;
import com.cadence.authservice.exception.TokenExpiredException;
import com.cadence.authservice.kafka.producer.AuthEventProducer;
import com.cadence.authservice.repository.PasswordResetTokenRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.impl.PasswordServiceImpl;
import com.cadence.authservice.util.TokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuthEventProducer eventProducer;
    @Mock private AuditLogService auditLogService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordService, "resetTokenExpiryMinutes", 30L);
    }

    @Test
    void forgotPassword_shouldSilentlyDoNothing_whenUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("ghost@email.com")).thenReturn(Optional.empty());

        passwordService.forgotPassword("ghost@email.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void forgotPassword_shouldGenerateTokenAndSendEmail_whenUserExists() {
        User user = User.builder().id(UUID.randomUUID()).email("jane@email.com").fullName("Jane").build();
        when(userRepository.findByEmailIgnoreCase("jane@email.com")).thenReturn(Optional.of(user));

        passwordService.forgotPassword("jane@email.com");

        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("jane@email.com"), eq("Jane"), any());
    }

    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(TokenGenerator.sha256("raw-token"))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword("raw-token", "NewStr0ng!Pass"))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void resetPassword_shouldThrow_whenTokenAlreadyUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(TokenGenerator.sha256("raw-token"))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true)
                .build();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordService.resetPassword("raw-token", "NewStr0ng!Pass"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndRevokeSessions_onValidToken() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(userId)
                .tokenHash(TokenGenerator.sha256("raw-token"))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        User user = User.builder().id(userId).email("jane@email.com").fullName("Jane").failedLoginAttempts(3).build();

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewStr0ng!Pass")).thenReturn("new-hashed-pw");

        passwordService.resetPassword("raw-token", "NewStr0ng!Pass");

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-pw");
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(refreshTokenService, times(1)).revokeAllForUser(userId);
        verify(eventProducer, times(1)).publishPasswordChanged(any());
    }

    private static String anyString() { return org.mockito.ArgumentMatchers.anyString(); }
    private static <T> T eq(T value) { return org.mockito.ArgumentMatchers.eq(value); }
}
