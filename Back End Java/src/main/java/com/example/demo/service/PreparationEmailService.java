package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.OrderPreparationEmailProperties;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dto.PreparationEmailStatusDto;
import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.EmailSenderService.EmailSendResult;
import com.example.demo.service.PreparationEmailHistoryService.ClaimResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreparationEmailService {

    private static final Logger log = LoggerFactory.getLogger(PreparationEmailService.class);
    private static final List<StatutCommande> PDF_STATUSES =
            List.of(StatutCommande.IN_PREP, StatutCommande.READY);
    private static final DateTimeFormatter SUBJECT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OrderPreparationEmailProperties properties;
    private final CreneauCollecteRepository creneauCollecteRepository;
    private final CommandeRepository commandeRepository;
    private final PreparationGroupService preparationGroupService;
    private final PreparationEmailHistoryService historyService;
    private final EmailSenderService emailSenderService;

    public Optional<PreparationEmailStatusDto> sendAutomatically(Long creneauId) {
        if (!properties.isConfigured()) {
            logConfigurationIssue();
            return Optional.empty();
        }

        CreneauCollecte creneau = getCreneau(creneauId);
        long orderCount = countRelevantOrders(creneauId);
        if (orderCount == 0) {
            log.info("Aucun dossier email genere pour le creneau {}: aucune commande pertinente.", creneauId);
            return Optional.empty();
        }

        return Optional.of(send(creneau, orderCount, false));
    }

    public PreparationEmailStatusDto sendManually(Long creneauId) {
        CreneauCollecte creneau = getCreneau(creneauId);
        long orderCount = countRelevantOrders(creneauId);
        if (orderCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Aucune commande en preparation ou prete pour ce creneau."
            );
        }
        if (!properties.isConfigured()) {
            logConfigurationIssue();
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "L'envoi email du dossier de preparation n'est pas configure."
            );
        }
        return send(creneau, orderCount, true);
    }

    public PreparationEmailStatusDto getStatus(Long creneauId) {
        getCreneau(creneauId);
        return withConfiguration(historyService.findStatus(creneauId)
                .orElseGet(() -> PreparationEmailStatusDto.builder()
                        .creneauId(creneauId)
                        .build()));
    }

    private PreparationEmailStatusDto send(
            CreneauCollecte creneau,
            long orderCount,
            boolean manual
    ) {
        String recipient = properties.getRecipient().trim();
        String subject = buildSubject(creneau);
        String body = buildBody(creneau, orderCount);
        ClaimResult claim;
        try {
            claim = manual
                    ? historyService.claimManual(creneau.getId(), recipient, subject, body)
                    : historyService.claimAutomatic(creneau.getId(), recipient, subject, body);
        } catch (DataIntegrityViolationException exception) {
            log.info("Envoi du dossier deja reserve pour le creneau {}.", creneau.getId());
            return getStatus(creneau.getId());
        }

        if (!claim.acquired()) {
            return getStatus(creneau.getId());
        }

        EmailSendResult sendResult;
        try {
            PreparationPdfDocument pdf = preparationGroupService.generatePdf(creneau.getId());
            sendResult = emailSenderService.sendHtmlEmailWithPdf(
                    recipient,
                    subject,
                    body,
                    pdf.fileName(),
                    pdf.content()
            );
        } catch (RuntimeException exception) {
            log.warn("Generation ou envoi du dossier impossible pour le creneau {}: {}",
                    creneau.getId(), exception.getMessage());
            sendResult = EmailSendResult.failed(LocalDateTime.now(), safeError(exception));
        }

        return withConfiguration(historyService.complete(claim.emailId(), sendResult));
    }

    private CreneauCollecte getCreneau(Long creneauId) {
        return creneauCollecteRepository.findById(creneauId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Creneau de collecte introuvable."
                ));
    }

    private long countRelevantOrders(Long creneauId) {
        return commandeRepository.countByCreneauCollecte_IdAndStatutIn(creneauId, PDF_STATUSES);
    }

    private String buildSubject(CreneauCollecte creneau) {
        return "Terra Sana - Dossier de preparation - "
                + creneau.getPointCollecte().getNom()
                + " - "
                + LocalDateTime.of(creneau.getCreneau(), creneau.getHeureDebut()).format(SUBJECT_DATE_FORMAT);
    }

    private String buildBody(CreneauCollecte creneau, long orderCount) {
        return """
                <!doctype html>
                <html lang="fr">
                  <body style="font-family:Arial,sans-serif;color:#202a26;line-height:1.5">
                    <p>Bonjour,</p>
                    <p>Veuillez trouver en pièce jointe le dossier de préparation pour le créneau suivant :</p>
                    <ul>
                      <li><strong>Point de collecte :</strong> %s</li>
                      <li><strong>Date :</strong> %s</li>
                      <li><strong>Créneau :</strong> %s - %s</li>
                      <li><strong>Nombre de commandes :</strong> %d</li>
                    </ul>
                    <p>Le document contient la pick-list globale ainsi que le détail des commandes à préparer.</p>
                    <p>Terra Sana</p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(creneau.getPointCollecte().getNom()),
                creneau.getCreneau().format(DATE_FORMAT),
                creneau.getHeureDebut().format(TIME_FORMAT),
                creneau.getHeureFin().format(TIME_FORMAT),
                orderCount
        );
    }

    private PreparationEmailStatusDto withConfiguration(PreparationEmailStatusDto status) {
        status.setEnvoiConfigure(properties.isConfigured());
        return status;
    }

    private void logConfigurationIssue() {
        if (!properties.isEnabled()) {
            log.info("Envoi automatique du dossier de preparation desactive par configuration.");
        } else {
            log.warn("Envoi automatique du dossier de preparation impossible: destinataire non configure.");
        }
    }

    private String safeError(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
