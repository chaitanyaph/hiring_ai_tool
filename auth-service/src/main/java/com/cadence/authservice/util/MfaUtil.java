package com.cadence.authservice.util;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Thin wrapper around the samstevens TOTP library implementing RFC 6238.
 * Using a well-audited library rather than hand-rolling HMAC-based OTP
 * avoids subtle timing/clock-drift bugs that are easy to get wrong.
 */
@Component
public class MfaUtil {

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    public String buildOtpAuthUrl(String issuer, String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    public String generateQrCodeBase64(String issuer, String email, String secret) {
        try {
            QrData data = new QrData.Builder()
                    .label(email)
                    .secret(secret)
                    .issuer(issuer)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();
            byte[] imageData = new ZxingPngQrGenerator().generate(data);
            return Base64.getEncoder().encodeToString(imageData);
        } catch (Exception e) {
            // QR generation is a convenience for the client UI; failure here
            // must never block MFA setup, since the secret/otpAuthUrl are still returned.
            return null;
        }
    }
}
