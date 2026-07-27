package com.cadence.authservice.util;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates opaque, cryptographically random tokens for refresh /
 * password-reset / email-verification flows, and hashes them before
 * persistence. We hash with SHA-256 (fast, deterministic, one-way) --
 * unlike passwords, these tokens are high-entropy random values, so a
 * slow KDF like BCrypt would only add unnecessary latency without a
 * meaningful security benefit.
 */
public final class TokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGenerator() {}

    public static String generateOpaqueToken() {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static String generateNumericOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String generateRecoveryCode() {
        // Format: XXXX-XXXX-XXXX for easy manual transcription
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[9]).toUpperCase();
        byte[] bytes = new byte[6];
        SECURE_RANDOM.nextBytes(bytes);
        String alnum = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        while (alnum.length() < 12) {
            alnum += "X";
        }
        return alnum.substring(0, 4) + "-" + alnum.substring(4, 8) + "-" + alnum.substring(8, 12);
    }
}
