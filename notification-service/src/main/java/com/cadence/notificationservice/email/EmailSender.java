package com.cadence.notificationservice.email;

import com.cadence.notificationservice.entity.EmailAttachment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.List;

/** Thin wrapper over JavaMailSender -- attachments are sent inline from LONGBLOB bytes, see EmailAttachment javadoc for why no object storage is used. */
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from-address}")
    private String fromAddress;

    @Value("${notification.email.from-name}")
    private String fromName;

    public void send(String toEmail, String subject, String bodyHtml, List<EmailAttachment> attachments)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        boolean hasAttachments = attachments != null && !attachments.isEmpty();
        MimeMessageHelper helper = new MimeMessageHelper(message, hasAttachments, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(bodyHtml, true);

        if (hasAttachments) {
            for (EmailAttachment attachment : attachments) {
                helper.addAttachment(attachment.getFileName(),
                        new ByteArrayResource(attachment.getContent()), attachment.getContentType());
            }
        }

        mailSender.send(message);
    }
}
