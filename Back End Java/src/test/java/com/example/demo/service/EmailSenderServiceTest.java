package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
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

    @Test
    void sendHtmlEmailWithPdfCreatesMultipartPdfAttachment() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        byte[] pdf = "%PDF-test-content".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(applicationContext.getBeanProvider(JavaMailSender.class)).thenReturn(mailSenderProvider);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);

        EmailSenderService.EmailSendResult result = emailSenderService.sendHtmlEmailWithPdf(
                "preparation@terrasana.test",
                "Dossier de preparation",
                "<p>Bonjour</p>",
                "preparation_Uccle_2026-10-10_14h00.pdf",
                pdf
        );

        assertThat(result.sent()).isTrue();
        verify(mailSender).send(message);
        message.saveChanges();
        BodyPart attachment = findAttachment((Multipart) message.getContent());
        assertThat(attachment).isNotNull();
        assertThat(attachment.getFileName()).isEqualTo("preparation_Uccle_2026-10-10_14h00.pdf");
        assertThat(attachment.getContentType()).startsWith("application/pdf");
        assertThat(readAllBytes(attachment.getInputStream())).isEqualTo(pdf);
    }

    @Test
    void sendHtmlEmailWithPdfReturnsFailedWhenSmtpFails() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(applicationContext.getBeanProvider(JavaMailSender.class)).thenReturn(mailSenderProvider);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP indisponible"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        EmailSenderService.EmailSendResult result = emailSenderService.sendHtmlEmailWithPdf(
                "preparation@terrasana.test",
                "Dossier de preparation",
                "<p>Bonjour</p>",
                "preparation.pdf",
                "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );

        assertThat(result.sent()).isFalse();
        assertThat(result.sentAt()).isNull();
        assertThat(result.errorMessage()).contains("SMTP indisponible");
    }

    @Test
    void sendHtmlEmailStillCreatesNonMultipartMessage() throws Exception {
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
        assertThat(message.getContent()).isInstanceOf(String.class);
    }

    private BodyPart findAttachment(Multipart multipart) throws Exception {
        for (int index = 0; index < multipart.getCount(); index++) {
            BodyPart part = multipart.getBodyPart(index);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                return part;
            }
            Object content = part.getContent();
            if (content instanceof Multipart nested) {
                BodyPart found = findAttachment(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private byte[] readAllBytes(InputStream input) throws Exception {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }
}
