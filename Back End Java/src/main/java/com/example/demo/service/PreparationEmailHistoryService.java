package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.dto.PreparationEmailStatusDto;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.service.EmailSenderService.EmailSendResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreparationEmailHistoryService {

    private static final EmailNotificationType TYPE = EmailNotificationType.PREPARATION_DOSSIER;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final GeneratedEmailRepository generatedEmailRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claimAutomatic(
            Long creneauId,
            String recipient,
            String subject,
            String htmlBody
    ) {
        Optional<GeneratedEmail> existing = generatedEmailRepository.findByTypeAndCreneauCollecteId(
                TYPE,
                creneauId
        );
        if (existing.isPresent()) {
            return ClaimResult.notAcquired(existing.get().getId(), existing.get().getStatutEnvoi());
        }
        GeneratedEmail created = generatedEmailRepository.saveAndFlush(newPendingEmail(
                creneauId,
                recipient,
                subject,
                htmlBody
        ));
        return ClaimResult.acquired(created.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimResult claimManual(
            Long creneauId,
            String recipient,
            String subject,
            String htmlBody
    ) {
        Optional<GeneratedEmail> existing = generatedEmailRepository.findByTypeAndCreneauCollecteIdForUpdate(
                TYPE,
                creneauId
        );
        if (existing.isEmpty()) {
            GeneratedEmail created = generatedEmailRepository.saveAndFlush(newPendingEmail(
                    creneauId,
                    recipient,
                    subject,
                    htmlBody
            ));
            return ClaimResult.acquired(created.getId());
        }

        GeneratedEmail email = existing.get();
        if (email.getStatutEnvoi() == EmailSendStatus.PENDING) {
            return ClaimResult.notAcquired(email.getId(), EmailSendStatus.PENDING);
        }

        email.setDestinataire(recipient);
        email.setSujet(subject);
        email.setContenuHtml(htmlBody);
        email.setStatutEnvoi(EmailSendStatus.PENDING);
        email.setDateTentativeEnvoi(null);
        email.setDateEnvoi(null);
        email.setErreurEnvoi(null);
        generatedEmailRepository.save(email);
        return ClaimResult.acquired(email.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PreparationEmailStatusDto complete(Long emailId, EmailSendResult result) {
        GeneratedEmail email = generatedEmailRepository.findByIdForUpdate(emailId)
                .orElseThrow(() -> new IllegalStateException("Historique email de preparation introuvable."));
        email.setDateTentativeEnvoi(result.attemptedAt());
        if (result.sent()) {
            email.setStatutEnvoi(EmailSendStatus.SENT);
            email.setDateEnvoi(result.sentAt());
            email.setErreurEnvoi(null);
        } else {
            email.setStatutEnvoi(EmailSendStatus.FAILED);
            email.setDateEnvoi(null);
            email.setErreurEnvoi(truncate(result.errorMessage()));
        }
        return toDto(generatedEmailRepository.save(email));
    }

    @Transactional(readOnly = true)
    public Optional<PreparationEmailStatusDto> findStatus(Long creneauId) {
        return generatedEmailRepository.findByTypeAndCreneauCollecteId(TYPE, creneauId)
                .map(this::toDto);
    }

    private GeneratedEmail newPendingEmail(
            Long creneauId,
            String recipient,
            String subject,
            String htmlBody
    ) {
        return GeneratedEmail.builder()
                .type(TYPE)
                .destinataire(recipient)
                .sujet(subject)
                .contenuHtml(htmlBody)
                .statutEnvoi(EmailSendStatus.PENDING)
                .creneauCollecteId(creneauId)
                .build();
    }

    private PreparationEmailStatusDto toDto(GeneratedEmail email) {
        return PreparationEmailStatusDto.builder()
                .creneauId(email.getCreneauCollecteId())
                .statutEnvoi(email.getStatutEnvoi())
                .dateTentativeEnvoi(email.getDateTentativeEnvoi())
                .dateEnvoi(email.getDateEnvoi())
                .erreurEnvoi(email.getErreurEnvoi())
                .build();
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Echec de l'envoi du dossier de preparation.";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > MAX_ERROR_LENGTH
                ? normalized.substring(0, MAX_ERROR_LENGTH)
                : normalized;
    }

    public record ClaimResult(boolean acquired, Long emailId, EmailSendStatus currentStatus) {
        static ClaimResult acquired(Long emailId) {
            return new ClaimResult(true, emailId, EmailSendStatus.PENDING);
        }

        static ClaimResult notAcquired(Long emailId, EmailSendStatus status) {
            return new ClaimResult(false, emailId, status);
        }
    }
}
