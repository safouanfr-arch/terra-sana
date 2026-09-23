package com.example.demo.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final String MAIL_SENDER_CLASS = "org.springframework.mail.javamail.JavaMailSender";
    private static final String MIME_MESSAGE_CLASS = "jakarta.mail.internet.MimeMessage";
    private static final String MIME_MESSAGE_HELPER_CLASS = "org.springframework.mail.javamail.MimeMessageHelper";

    private final ApplicationContext applicationContext;

    @Value("${app.mail.from:no-reply@terrasana.local}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailSendResult sendHtmlEmail(String destinataire, String sujet, String contenuHtml) {
        LocalDateTime attemptedAt = LocalDateTime.now();

        if (!mailEnabled) {
            String message = "Envoi SMTP desactive pour cet environnement.";
            log.info(message);
            return EmailSendResult.failed(attemptedAt, message);
        }

        if (isBlank(destinataire)) {
            return EmailSendResult.failed(attemptedAt, "Destinataire email absent.");
        }

        if (isBlank(fromAddress)) {
            return EmailSendResult.failed(attemptedAt, "Adresse expediteur SMTP absente.");
        }

        Object mailSender = resolveMailSender(attemptedAt);
        if (mailSender == null) {
            return EmailSendResult.failed(
                    attemptedAt,
                    "Spring Mail indisponible au demarrage. Lancez avec Gradle ou actualisez le projet Gradle dans l'IDE."
            );
        }

        try {
            Object message = invokeNoArg(mailSender, "createMimeMessage");
            Object helper = createMimeMessageHelper(message);

            invokeString(helper, "setFrom", fromAddress.trim());
            invokeString(helper, "setTo", destinataire.trim());
            invokeString(helper, "setSubject", sujet != null ? sujet : "");
            invokeTextHtml(helper, contenuHtml != null ? contenuHtml : "");
            invokeSend(mailSender, message);

            return EmailSendResult.sent(attemptedAt, LocalDateTime.now());
        } catch (InvocationTargetException exception) {
            String error = sanitizeError(exception.getTargetException());
            log.warn("Echec envoi email vers {}: {}", destinataire, error);
            return EmailSendResult.failed(attemptedAt, error);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            String error = sanitizeError(exception);
            log.warn("Echec envoi email vers {}: {}", destinataire, error);
            return EmailSendResult.failed(attemptedAt, error);
        }
    }

    private Object resolveMailSender(LocalDateTime attemptedAt) {
        try {
            Class<?> mailSenderClass = Class.forName(MAIL_SENDER_CLASS);
            ObjectProvider<?> provider = applicationContext.getBeanProvider(mailSenderClass);
            return provider != null ? provider.getIfAvailable() : null;
        } catch (ClassNotFoundException exception) {
            log.warn("Spring Mail absent du classpath a {}.", attemptedAt);
            return null;
        } catch (RuntimeException exception) {
            log.warn("Envoi email impossible a {}: {}", attemptedAt, sanitizeError(exception));
            return null;
        }
    }

    private Object invokeNoArg(Object target, String methodName)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private Object createMimeMessageHelper(Object message)
            throws ReflectiveOperationException {
        Class<?> mimeMessageClass = Class.forName(MIME_MESSAGE_CLASS);
        Class<?> helperClass = Class.forName(MIME_MESSAGE_HELPER_CLASS);
        Constructor<?> constructor = helperClass.getConstructor(mimeMessageClass, String.class);
        return constructor.newInstance(message, "UTF-8");
    }

    private void invokeString(Object target, String methodName, String value)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, String.class);
        method.invoke(target, value);
    }

    private void invokeTextHtml(Object target, String html)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod("setText", String.class, boolean.class);
        method.invoke(target, html, true);
    }

    private void invokeSend(Object mailSender, Object message)
            throws ReflectiveOperationException {
        Class<?> mimeMessageClass = Class.forName(MIME_MESSAGE_CLASS);
        Method method = mailSender.getClass().getMethod("send", mimeMessageClass);
        method.invoke(mailSender, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String sanitizeError(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }

    public record EmailSendResult(
            boolean sent,
            LocalDateTime attemptedAt,
            LocalDateTime sentAt,
            String errorMessage
    ) {
        public static EmailSendResult sent(LocalDateTime attemptedAt, LocalDateTime sentAt) {
            return new EmailSendResult(true, attemptedAt, sentAt, null);
        }

        public static EmailSendResult failed(LocalDateTime attemptedAt, String errorMessage) {
            return new EmailSendResult(false, attemptedAt, null, errorMessage);
        }
    }

}
