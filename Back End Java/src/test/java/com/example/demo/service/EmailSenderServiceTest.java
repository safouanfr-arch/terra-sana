package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private EmailSenderService emailSenderService;

    @BeforeEach
    void setUp() {
        emailSenderService = new EmailSenderService(applicationContext);
        ReflectionTestUtils.setField(emailSenderService, "fromAddress", "no-reply@terrasana.test");
        ReflectionTestUtils.setField(emailSenderService, "mailEnabled", true);
    }

    @Test
    void sendHtmlEmailCallsJavaMailSenderWhenSuccessful() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(applicationContext.getBeanProvider(JavaMailSender.class)).thenReturn(mailSenderProvider);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        EmailSenderService.EmailSendResult result = emailSenderService.sendHtmlEmail(
                "membre@terrasana.test",
                "Confirmation",
                "<p>Bonjour</p>"
        );

        assertThat(result.sent()).isTrue();
        assertThat(result.attemptedAt()).isNotNull();
        assertThat(result.sentAt()).isNotNull();
        assertThat(result.errorMessage()).isNull();
        verify(mailSender).send(message);
    }

    @Test
    void sendHtmlEmailReturnsFailedWhenSmtpFails() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(applicationContext.getBeanProvider(JavaMailSender.class)).thenReturn(mailSenderProvider);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP indisponible"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        EmailSenderService.EmailSendResult result = emailSenderService.sendHtmlEmail(
                "membre@terrasana.test",
                "Confirmation",
                "<p>Bonjour</p>"
        );

        assertThat(result.sent()).isFalse();
        assertThat(result.attemptedAt()).isNotNull();
        assertThat(result.sentAt()).isNull();
        assertThat(result.errorMessage()).contains("SMTP indisponible");
    }

    @Test
    void sendHtmlEmailReturnsFailedWithoutCrashingWhenNoMailSenderIsAvailable() {
        when(applicationContext.getBeanProvider(JavaMailSender.class)).thenReturn(mailSenderProvider);
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        EmailSenderService.EmailSendResult result = emailSenderService.sendHtmlEmail(
                "membre@terrasana.test",
                "Confirmation",
                "<p>Bonjour</p>"
        );

        assertThat(result.sent()).isFalse();
        assertThat(result.attemptedAt()).isNotNull();
        assertThat(result.sentAt()).isNull();
        assertThat(result.errorMessage()).contains("Spring Mail indisponible");
    }
}
