package com.amins.nphies.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Sends transactional HTML emails (invitation and password-reset OTPs).
 *
 * Disabled by default (app.mail.enabled=false): in that mode the send methods
 * return false and callers fall back to their own delivery strategy. A send
 * failure also returns false — business flows must never be blocked by SMTP
 * trouble. Callers are expected to invoke this AFTER their DB transaction has
 * committed so slow SMTP servers cannot hold transactions open.
 */
@Service
@Slf4j
public class EmailService {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm 'UTC'", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String inviteTemplate;
    private final String resetTemplate;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled}") boolean enabled,
                        @Value("${app.mail.from}") String from) {
        this.mailSender     = mailSender;
        this.enabled        = enabled;
        this.from           = from;
        this.inviteTemplate = loadTemplate("templates/invite-email.html");
        this.resetTemplate  = loadTemplate("templates/reset-password-email.html");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return true if sent, false if mail is disabled or sending failed
     *         (caller should fall back to manual OTP delivery)
     */
    public boolean sendInvite(String toEmail, String otp, OffsetDateTime expiresAt) {
        return send(toEmail, "Your TraCare Claim invitation code",
                fill(inviteTemplate, toEmail, otp, expiresAt));
    }

    /**
     * @return true if sent, false if mail is disabled or sending failed
     */
    public boolean sendPasswordReset(String toEmail, String otp, OffsetDateTime expiresAt) {
        return send(toEmail, "Your TraCare Claim password reset code",
                fill(resetTemplate, toEmail, otp, expiresAt));
    }

    private boolean send(String toEmail, String subject, String html) {
        if (!enabled) {
            log.debug("Mail disabled — skipping email '{}' to {}", subject, toEmail);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email '{}' to {}", subject, toEmail, e);
            return false;
        }
    }

    static String fill(String template, String toEmail, String otp, OffsetDateTime expiresAt) {
        return template
                .replace("{{EMAIL}}", toEmail)
                .replace("{{OTP}}", otp)
                .replace("{{EXPIRES}}", expiresAt.withOffsetSameInstant(ZoneOffset.UTC).format(EXPIRY_FORMAT));
    }

    String buildInviteHtml(String toEmail, String otp, OffsetDateTime expiresAt) {
        return fill(inviteTemplate, toEmail, otp, expiresAt);
    }

    String buildPasswordResetHtml(String toEmail, String otp, OffsetDateTime expiresAt) {
        return fill(resetTemplate, toEmail, otp, expiresAt);
    }

    private static String loadTemplate(String path) {
        try {
            return new String(
                    new ClassPathResource(path).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load email template: " + path, e);
        }
    }
}
