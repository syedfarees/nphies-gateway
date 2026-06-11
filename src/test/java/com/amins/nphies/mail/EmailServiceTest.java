package com.amins.nphies.mail;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private static final OffsetDateTime EXPIRES =
            OffsetDateTime.of(2026, 6, 13, 15, 30, 0, 0, ZoneOffset.UTC);

    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new JavaMailSenderImpl().createMimeMessage());
    }

    @Test
    void buildInviteHtml_substitutesAllPlaceholders() {
        EmailService service = new EmailService(mailSender, true, "no-reply@tracare.sa");

        String html = service.buildInviteHtml("doctor@hospital.sa", "123456", EXPIRES);

        assertThat(html)
                .contains("doctor@hospital.sa")
                .contains("123456")
                .contains("13 June 2026, 15:30 UTC")
                .doesNotContain("{{");
    }

    @Test
    void buildPasswordResetHtml_substitutesAllPlaceholders() {
        EmailService service = new EmailService(mailSender, true, "no-reply@tracare.sa");

        String html = service.buildPasswordResetHtml("doctor@hospital.sa", "654321", EXPIRES);

        assertThat(html)
                .contains("doctor@hospital.sa")
                .contains("654321")
                .contains("13 June 2026, 15:30 UTC")
                .contains("Password reset requested")
                .doesNotContain("{{");
    }

    @Test
    void send_whenDisabled_returnsFalseWithoutSending() {
        EmailService service = new EmailService(mailSender, false, "no-reply@tracare.sa");

        assertThat(service.sendInvite("doctor@hospital.sa", "123456", EXPIRES)).isFalse();
        assertThat(service.sendPasswordReset("doctor@hospital.sa", "123456", EXPIRES)).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_whenEnabled_sendsAndReturnsTrue() {
        EmailService service = new EmailService(mailSender, true, "no-reply@tracare.sa");

        assertThat(service.sendInvite("doctor@hospital.sa", "123456", EXPIRES)).isTrue();
        assertThat(service.sendPasswordReset("doctor@hospital.sa", "123456", EXPIRES)).isTrue();
        verify(mailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void send_whenSmtpFails_returnsFalseInsteadOfThrowing() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));
        EmailService service = new EmailService(mailSender, true, "no-reply@tracare.sa");

        assertThat(service.sendInvite("doctor@hospital.sa", "123456", EXPIRES)).isFalse();
        assertThat(service.sendPasswordReset("doctor@hospital.sa", "123456", EXPIRES)).isFalse();
    }
}
