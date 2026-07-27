package com.cadence.authservice.service.impl;

import com.cadence.authservice.constant.AuditEventType;
import com.cadence.authservice.dto.response.MfaSetupResponse;
import com.cadence.authservice.entity.MfaSecret;
import com.cadence.authservice.entity.User;
import com.cadence.authservice.exception.InvalidCredentialsException;
import com.cadence.authservice.exception.InvalidMfaCodeException;
import com.cadence.authservice.exception.MfaAlreadyEnabledException;
import com.cadence.authservice.exception.ResourceNotFoundException;
import com.cadence.authservice.repository.MfaSecretRepository;
import com.cadence.authservice.repository.UserRepository;
import com.cadence.authservice.service.AuditLogService;
import com.cadence.authservice.service.MfaService;
import com.cadence.authservice.util.MfaUtil;
import com.cadence.authservice.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private static final String MFA_SESSION_PREFIX = "auth:mfa-session:";

    private final MfaSecretRepository mfaSecretRepository;
    private final UserRepository userRepository;
    private final MfaUtil mfaUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.mfa-issuer}")
    private String mfaIssuer;

    @Override
    @Transactional
    public MfaSetupResponse setupMfa(UUID userId, String email) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isMfaEnabled()) {
            throw new MfaAlreadyEnabledException();
        }

        String secret = mfaUtil.generateSecret();
        List<String> recoveryCodes = IntStream.range(0, 8)
                .mapToObj(i -> TokenGenerator.generateRecoveryCode())
                .collect(Collectors.toList());
        String hashedRecoveryCodes = recoveryCodes.stream()
                .map(TokenGenerator::sha256)
                .collect(Collectors.joining(","));

        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
                .orElse(MfaSecret.builder().userId(userId).build());
        mfaSecret.setSecretKey(secret);
        mfaSecret.setConfirmed(false);
        mfaSecret.setRecoveryCodes(hashedRecoveryCodes);
        mfaSecretRepository.save(mfaSecret);

        String otpAuthUrl = mfaUtil.buildOtpAuthUrl(mfaIssuer, email, secret);
        String qrBase64 = mfaUtil.generateQrCodeBase64(mfaIssuer, email, secret);

        return MfaSetupResponse.builder()
                .secret(secret)
                .otpAuthUrl(otpAuthUrl)
                .qrCodeImageBase64(qrBase64)
                .recoveryCodes(recoveryCodes)
                .build();
    }

    @Override
    @Transactional
    public void confirmMfaSetup(UUID userId, String code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending MFA setup found for this user"));

        if (!mfaUtil.verifyCode(mfaSecret.getSecretKey(), code)) {
            throw new InvalidMfaCodeException();
        }

        mfaSecret.setConfirmed(true);
        mfaSecretRepository.save(mfaSecret);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setMfaEnabled(true);
        userRepository.save(user);

        auditLogService.record(userId, AuditEventType.MFA_ENABLED, "MFA enabled and confirmed", null, null);
    }

    @Override
    @Transactional
    public void disableMfa(UUID userId, String currentPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setMfaEnabled(false);
        userRepository.save(user);
        mfaSecretRepository.findByUserId(userId).ifPresent(mfaSecretRepository::delete);

        auditLogService.record(userId, AuditEventType.MFA_DISABLED, "MFA disabled by user", null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyCode(UUID userId, String code) {
        MfaSecret mfaSecret = mfaSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("MFA is not configured for this user"));
        return mfaUtil.verifyCode(mfaSecret.getSecretKey(), code);
    }

    @Override
    public String issueMfaSessionToken(UUID userId) {
        String sessionToken = TokenGenerator.generateOpaqueToken();
        redisTemplate.opsForValue().set(MFA_SESSION_PREFIX + sessionToken, userId.toString(), Duration.ofMinutes(5));
        return sessionToken;
    }

    @Override
    public UUID resolveMfaSessionToken(String mfaSessionToken) {
        String userId = redisTemplate.opsForValue().get(MFA_SESSION_PREFIX + mfaSessionToken);
        if (userId == null) {
            throw new InvalidMfaCodeException();
        }
        return UUID.fromString(userId);
    }
}
