package com.cadence.authservice.service.impl;

import com.cadence.authservice.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * NOTE: In the full platform, transactional emails are actually sent by
 * the dedicated Notification Service, subscribed to Kafka topics this
 * service publishes (see AuthEventProducer). This EmailService exists
 * as a direct-send fallback/local-dev path and for flows that need a
 * synchronous "email sent" guarantee independent of Kafka availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String link = frontendBaseUrl + "/verify-email?token=" + token;
        log.debug("Verification link for {} (dev visibility, in case SMTP isn't configured): {}", toEmail, link);
        String body = "Hi " + fullName + ",\n\nWelcome to Cadence! Please verify your email by clicking the link below:\n"
                + link + "\n\nThis link expires in 24 hours.\n\n- Cadence Team";
        send(toEmail, "Verify your Cadence account", body);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String link = frontendBaseUrl + "/reset-password?token=" + token;
        log.debug("Password reset link for {} (dev visibility, in case SMTP isn't configured): {}", toEmail, link);
        String body = "Hi " + fullName + ",\n\nWe received a request to reset your password. Click below to proceed:\n"
                + link + "\n\nThis link expires in 30 minutes. If you did not request this, please ignore this email.\n\n- Cadence Team";
        send(toEmail, "Reset your Cadence password", body);
    }

    @Override
    @Async
    public void sendAccountLockedEmail(String toEmail, String fullName) {
        String body = "Hi " + fullName + ",\n\nYour account has been temporarily locked due to multiple failed login attempts. "
                + "If this wasn't you, please reset your password immediately.\n\n- Cadence Team";
        send(toEmail, "Your Cadence account has been locked", body);
    }

    @Override
    @Async
    public void sendPasswordChangedNotification(String toEmail, String fullName) {
        String body = "Hi " + fullName + ",\n\nYour password was just changed. If you did not make this change, "
                + "please contact support immediately.\n\n- Cadence Team";
        send(toEmail, "Your Cadence password was changed", body);
    }

    private void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            // Local/dev environments often lack real SMTP credentials --
            // never let email failure break the auth flow itself.
            log.warn("Email send skipped/failed (non-fatal) for {}: {}", to, e.getMessage());
        }
    }
}
