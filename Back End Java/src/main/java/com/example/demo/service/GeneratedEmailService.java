package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.dto.EmailSettingsDto;
import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.dto.EmailTemplatePreviewRequestDto;
import com.example.demo.dto.GeneratedEmailDto;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Commande;
import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.EmailSenderService.EmailSendResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneratedEmailService {

    private static final BigDecimal LOW_STOCK_THRESHOLD = new BigDecimal("5.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final GeneratedEmailRepository generatedEmailRepository;
    private final EmailTemplateService emailTemplateService;
    private final EmailSettingsService emailSettingsService;
    private final EmailSenderService emailSenderService;
    private final AdministrateurRepository administrateurRepository;
    private final CommandeRepository commandeRepository;

    @Transactional(readOnly = true)
    public List<GeneratedEmailDto> getAll() {
        return generatedEmailRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public String previewGeneratedEmail(Long id) {
        GeneratedEmail email = getEntity(id);
        return buildPreviewPage(
                email.getSujet(),
                email.getDestinataire(),
                email.getContenuHtml(),
                email.getCreatedAt(),
                true
        );
    }

    @Transactional(readOnly = true)
    public String previewTemplate(EmailNotificationType type, EmailTemplatePreviewRequestDto request) {
        Map<String, String> variables = buildPreviewVariables(request != null ? request.getVariables() : null);
        EmailTemplateDto overrideTemplate = request == null ? null : EmailTemplateDto.builder()
                .type(type.name())
                .objet(request.getObjet())
                .contenu(request.getContenu())
                .active(request.getActive())
                .build();

        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTemplate(type, variables, overrideTemplate);
        return buildPreviewPage(
                rendered.subject(),
                variables.getOrDefault("destinataire", variables.getOrDefault("companyEmail", "preview@local")),
                normalizeBodyHtml(rendered.body()),
                LocalDateTime.now(),
                false
        );
    }

    @Transactional
    public GeneratedEmailDto sendOrderReminder(Long commandeId) {
        Commande commande = getOrderForReminder(commandeId);
        if (isReminderExcluded(commande)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun rappel ne peut etre envoye pour une commande annulee ou terminee.");
        }
        if (hasOrderReminder(commandeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un rappel J-1 a deja ete genere pour cette commande.");
        }
        return generateOrderEmail(EmailNotificationType.ORDER_REMINDER_J1, commande);
    }

    @Transactional
    public Optional<GeneratedEmailDto> sendOrderReminderIfDue(Long commandeId, LocalDate reminderDate) {
        Commande commande = getOrderForReminder(commandeId);
        if (isReminderExcluded(commande)
                || commande.getDateRetraitPrevu() == null
                || !commande.getDateRetraitPrevu().toLocalDate().equals(reminderDate)
                || hasOrderReminder(commandeId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(generateOrderEmail(EmailNotificationType.ORDER_REMINDER_J1, commande));
    }

    private Commande getOrderForReminder(Long commandeId) {
        return commandeRepository.findByIdForUpdate(commandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable."));
    }

    private boolean isReminderExcluded(Commande commande) {
        return commande.getStatut() == StatutCommande.CANCELLED
                || commande.getStatut() == StatutCommande.DISTRIBUTED;
    }

    private boolean hasOrderReminder(Long commandeId) {
        return generatedEmailRepository.existsByTypeAndCommandeId(
                EmailNotificationType.ORDER_REMINDER_J1,
                commandeId
        );
    }

    @Transactional
    public GeneratedEmailDto generateOrderConfirmation(Commande commande) {
        return generateOrderEmail(EmailNotificationType.ORDER_CONFIRMATION, commande);
    }

    @Transactional
    public GeneratedEmailDto generateOrderCancellation(Commande commande) {
        return generateOrderEmail(EmailNotificationType.ORDER_CANCELLATION, commande);
    }

    @Transactional
    public GeneratedEmailDto generateOrderReady(Commande commande) {
        return generateOrderEmail(EmailNotificationType.ORDER_READY, commande);
    }

    @Transactional
    public GeneratedEmailDto generateMembershipValidation(DemandeAdhesion demande, String lienMotDePasse) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", safe(demande.getPrenom()));
        variables.put("nom", safe(demande.getNom()));
        variables.put("lienMotDePasse", safe(lienMotDePasse));

        return generateSingleEmail(
                EmailNotificationType.MEMBERSHIP_VALIDATION,
                safe(demande.getEmail()),
                variables,
                new EmailReferences(null, null, null, demande.getId())
        );
    }

    @Transactional
    public GeneratedEmailDto generateMembershipRejection(DemandeAdhesion demande) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", safe(demande.getPrenom()));
        variables.put("nom", safe(demande.getNom()));
        variables.put("motifRefus", safe(demande.getMessageRefus()));

        return generateSingleEmail(
                EmailNotificationType.MEMBERSHIP_REJECTION,
                safe(demande.getEmail()),
                variables,
                new EmailReferences(null, null, null, demande.getId())
        );
    }

    @Transactional
    public GeneratedEmailDto generateMembershipRequestReceived(DemandeAdhesion demande) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", safe(demande.getPrenom()));
        variables.put("nom", safe(demande.getNom()));

        return generateSingleEmail(
                EmailNotificationType.MEMBERSHIP_REQUEST_RECEIVED,
                safe(demande.getEmail()),
                variables,
                new EmailReferences(null, null, null, demande.getId())
        );
    }

    @Transactional
    public GeneratedEmailDto generatePasswordSetup(Membre membre, String lienMotDePasse) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", safe(membre.getPrenom()));
        variables.put("nom", safe(membre.getNom()));
        variables.put("lienMotDePasse", safe(lienMotDePasse));

        return generateSingleEmail(
                EmailNotificationType.PASSWORD_SETUP,
                safe(membre.getEmail()),
                variables,
                new EmailReferences(null, membre.getId(), null, null)
        );
    }

    @Transactional
    public void generateStockAlertIfNeeded(Produit produit, BigDecimal stockAvant, BigDecimal stockApres) {
        BigDecimal before = scale(stockAvant);
        BigDecimal after = scale(stockApres);

        EmailNotificationType type = null;
        if (after.compareTo(BigDecimal.ZERO) <= 0 && before.compareTo(BigDecimal.ZERO) > 0) {
            type = EmailNotificationType.OUT_OF_STOCK_ALERT;
        } else if (after.compareTo(BigDecimal.ZERO) > 0
                && after.compareTo(LOW_STOCK_THRESHOLD) <= 0
                && before.compareTo(LOW_STOCK_THRESHOLD) > 0) {
            type = EmailNotificationType.LOW_STOCK_ALERT;
        }

        if (type == null) {
            return;
        }

        List<Administrateur> admins = administrateurRepository.findByActifTrue();
        if (admins.isEmpty()) {
            return;
        }

        for (Administrateur admin : admins) {
            Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
            variables.put("prenom", safe(admin.getPrenom()));
            variables.put("nom", safe(admin.getNom()));
            variables.put("produitNom", safe(produit.getNom()));
            variables.put("stockActuel", formatNumber(after));
            variables.put("seuilStock", formatNumber(LOW_STOCK_THRESHOLD));

            generateSingleEmail(
                    type,
                    safe(admin.getEmail()),
                    variables,
                    new EmailReferences(null, null, produit.getId(), null)
            );
        }
    }

    private GeneratedEmailDto generateOrderEmail(EmailNotificationType type, Commande commande) {
        Map<String, String> variables = buildOrderVariables(commande);
        return generateSingleEmail(
                type,
                safe(commande.getMembre().getEmail()),
                variables,
                new EmailReferences(commande.getId(), commande.getMembre().getId(), null, null)
        );
    }

    private GeneratedEmailDto generateSingleEmail(
            EmailNotificationType type,
            String destinataire,
            Map<String, String> variables,
            EmailReferences references
    ) {
        if (destinataire == null || destinataire.isBlank()) {
            return null;
        }
        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTemplate(type, variables, null);
        if (rendered.template().getActive() != null && !rendered.template().getActive()) {
            return null;
        }

        String finalBody = ensurePasswordLink(type, rendered.body(), variables);
        String normalizedBody = normalizeBodyHtml(finalBody);

        GeneratedEmail email = GeneratedEmail.builder()
                .type(type)
                .destinataire(destinataire)
                .sujet(rendered.subject())
                .contenuHtml(normalizedBody)
                .statutEnvoi(EmailSendStatus.PENDING)
                .commandeId(references.commandeId())
                .membreId(references.membreId())
                .produitId(references.produitId())
                .demandeAdhesionId(references.demandeAdhesionId())
                .build();

        email = generatedEmailRepository.save(email);
        EmailSettingsDto settings = getSafeEmailSettings();

        EmailSendResult sendResult = emailSenderService.sendHtmlEmail(
                email.getDestinataire(),
                email.getSujet(),
                buildEmailDocument(email.getSujet(), email.getDestinataire(), normalizedBody, null, null, settings)
        );
        applySendResult(email, sendResult);

        return toDto(generatedEmailRepository.save(email));
    }

    private String ensurePasswordLink(
            EmailNotificationType type,
            String body,
            Map<String, String> variables
    ) {
        if (type != EmailNotificationType.MEMBERSHIP_VALIDATION
                && type != EmailNotificationType.PASSWORD_SETUP) {
            return body;
        }

        String passwordLink = variables != null ? variables.get("lienMotDePasse") : null;
        if (passwordLink == null || passwordLink.isBlank()) {
            return body;
        }

        String existingBody = body == null ? "" : body;
        if (existingBody.contains(passwordLink)) {
            return existingBody;
        }

        return existingBody + "\n\nLien de definition du mot de passe :\n" + passwordLink;
    }

    private void applySendResult(GeneratedEmail email, EmailSendResult sendResult) {
        email.setDateTentativeEnvoi(sendResult.attemptedAt());
        if (sendResult.sent()) {
            email.setStatutEnvoi(EmailSendStatus.SENT);
            email.setDateEnvoi(sendResult.sentAt());
            email.setErreurEnvoi(null);
        } else {
            email.setStatutEnvoi(EmailSendStatus.FAILED);
            email.setDateEnvoi(null);
            email.setErreurEnvoi(truncateError(sendResult.errorMessage()));
        }
    }

    private GeneratedEmail getEntity(Long id) {
        return generatedEmailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email genere introuvable."));
    }

    private Map<String, String> buildOrderVariables(Commande commande) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", safe(commande.getMembre().getPrenom()));
        variables.put("nom", safe(commande.getMembre().getNom()));
        variables.put("numero", safe(commande.getNumero()));
        variables.put("montant", formatMoney(commande.getMontantTTC()));
        variables.put("codeRetrait", safe(commande.getCodeRetrait()));
        variables.put("pointCollecte", safe(commande.getPointCollecte() != null ? commande.getPointCollecte().getNom() : ""));
        variables.put("dateRetrait", formatDateTime(commande.getDateRetraitPrevu()));
        variables.put("creneauRetrait", formatCreneau(commande));
        variables.put("detailProduits", formatOrderLines(commande.getLignes()));
        variables.put("destinataire", safe(commande.getMembre().getEmail()));
        return variables;
    }

    private Map<String, String> buildPreviewVariables(Map<String, String> incomingVariables) {
        Map<String, String> variables = withCompanyVariables(new LinkedHashMap<>());
        variables.put("prenom", "Marie");
        variables.put("nom", "Dupont");
        variables.put("numero", "CMD-2026-001");
        variables.put("montant", "42,50 €");
        variables.put("codeRetrait", "TS-4821");
        variables.put("pointCollecte", "Point de collecte Bruxelles");
        variables.put("dateRetrait", "15/05/2026");
        variables.put("creneauRetrait", "17h00 - 19h00");
        variables.put("detailProduits", "<ul><li>Panier legumes bio x1</li><li>Jus de pomme artisanal x2</li></ul>");
        variables.put("lienMotDePasse", "http://localhost:5173/creation-mot-de-passe/demo");
        variables.put("produitNom", "Panier legumes bio");
        variables.put("stockActuel", "3");
        variables.put("seuilStock", "5");
        variables.put("motifRefus", "Informations insuffisantes");
        variables.put("destinataire", "marie.dupont@example.test");

        if (incomingVariables != null) {
            incomingVariables.forEach((key, value) -> variables.put(key, value != null ? value : ""));
        }

        return variables;
    }

    private Map<String, String> withCompanyVariables(Map<String, String> variables) {
        variables.putAll(emailSettingsService.getCompanyVariables());
        return variables;
    }

    private String formatOrderLines(List<LigneCommande> lignes) {
        if (lignes == null || lignes.isEmpty()) {
            return "<p>Aucun detail produit.</p>";
        }

        StringBuilder builder = new StringBuilder("""
                <div style="margin:18px 0 8px;">
                  <p style="margin:0 0 8px; color:#2f5a4f; font-weight:700;">Articles commandes</p>
                  <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%; border-collapse:collapse; border:1px solid #e5ded2; border-radius:10px; overflow:hidden;">
                """);
        for (LigneCommande ligne : lignes) {
            builder.append("""
                    <tr>
                      <td style="padding:10px 12px; border-bottom:1px solid #eee7dc; color:#1d252b;">
                    """)
                    .append(escapeHtml(ligne.getProduit() != null ? ligne.getProduit().getNom() : "Produit"))
                    .append("</td><td style=\"padding:10px 12px; border-bottom:1px solid #eee7dc; text-align:center; color:#65706b; white-space:nowrap;\">x")
                    .append(formatNumber(ligne.getQuantite()))
                    .append("</td><td style=\"padding:10px 12px; border-bottom:1px solid #eee7dc; text-align:right; color:#1d252b; font-weight:700; white-space:nowrap;\">")
                    .append(formatMoney(ligne.getMontantTTC()))
                    .append("</td></tr>");
        }
        builder.append("</table></div>");
        return builder.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_FORMAT) : "";
    }

    private String formatCreneau(Commande commande) {
        if (commande.getCreneauCollecte() == null) {
            return "";
        }
        return commande.getCreneauCollecte().getHeureDebut() + " - " + commande.getCreneauCollecte().getHeureFin();
    }

    private String formatMoney(BigDecimal amount) {
        return formatNumber(scale(amount)) + " EUR";
    }

    private String formatNumber(BigDecimal value) {
        return scale(value).stripTrailingZeros().toPlainString();
    }

    private BigDecimal scale(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeBodyHtml(String body) {
        if (body == null || body.isBlank()) {
            return "<p>Aucun contenu.</p>";
        }

        String normalized = body.replace("\r\n", "\n").replace("\r", "\n");
        String[] blocks = normalized.split("\\n\\n+");
        StringBuilder html = new StringBuilder();

        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("<")) {
                html.append(trimmed);
            } else if (isInfoBlock(trimmed)) {
                html.append(buildInfoBlock(trimmed));
            } else {
                html.append("<p style=\"margin:0 0 14px;\">")
                        .append(formatTextBlock(trimmed))
                        .append("</p>");
            }
        }

        return html.length() == 0 ? "<p>Aucun contenu.</p>" : html.toString();
    }

    private boolean isInfoBlock(String block) {
        String[] lines = block.split("\\n");
        if (lines.length < 2) {
            return false;
        }

        for (String line : lines) {
            String trimmed = line.trim();
            int separatorIndex = trimmed.indexOf(':');
            if (separatorIndex <= 0
                    || separatorIndex > 42
                    || trimmed.toLowerCase().startsWith("http")) {
                return false;
            }
        }
        return true;
    }

    private String buildInfoBlock(String block) {
        String[] lines = block.split("\\n");
        StringBuilder builder = new StringBuilder("""
                <div style="margin:18px 0; border:1px solid #e5ded2; border-radius:10px; overflow:hidden; background:#fbfaf7;">
                """);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int separatorIndex = line.indexOf(':');
            String label = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            String border = i + 1 < lines.length ? " border-bottom:1px solid #eee7dc;" : "";

            builder.append("<div style=\"display:flex; gap:14px; padding:10px 12px;")
                    .append(border)
                    .append("\"><span style=\"min-width:130px; color:#65706b;\">")
                    .append(escapeHtml(label))
                    .append("</span><strong style=\"color:#1d252b; font-weight:700;\">")
                    .append(formatTextBlock(value))
                    .append("</strong></div>");
        }

        builder.append("</div>");
        return builder.toString();
    }

    private String formatTextBlock(String block) {
        String[] lines = block.split("\\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append("<br>");
            }
            builder.append(formatTextLine(lines[i].trim()));
        }
        return builder.toString();
    }

    private String formatTextLine(String value) {
        if (value != null && (value.startsWith("http://") || value.startsWith("https://"))) {
            String escapedUrl = escapeHtml(value);
            return "<a href=\"" + escapedUrl + "\" style=\"color:#2f5a4f; font-weight:700;\">" + escapedUrl + "</a>";
        }
        return escapeHtml(value);
    }

    private String buildPreviewPage(
            String subject,
            String destinataire,
            String contentHtml,
            LocalDateTime date,
            boolean generated
    ) {
        String notice = generated
                ? "Apercu de l'email enregistre dans l'historique."
                : "Previsualisation du template - aucun envoi n'est declenche.";
        return buildEmailDocument(subject, destinataire, contentHtml, date, notice);
    }

    private String buildEmailDocument(
            String subject,
            String destinataire,
            String contentHtml,
            LocalDateTime date
    ) {
        return buildEmailDocument(subject, destinataire, contentHtml, date, null);
    }

    private String buildEmailDocument(
            String subject,
            String destinataire,
            String contentHtml,
            LocalDateTime date,
            String notice
    ) {
        EmailSettingsDto settings = getSafeEmailSettings();
        return buildEmailDocument(subject, destinataire, contentHtml, date, notice, settings);
    }

    private String buildEmailDocument(
            String subject,
            String destinataire,
            String contentHtml,
            LocalDateTime date,
            String notice,
            EmailSettingsDto settings
    ) {
        String companyName = defaultIfBlank(settings.getNomEntreprise(), "Terra Sana");
        String noticeHtml = notice == null || notice.isBlank()
                ? ""
                : "<p style=\"margin:0 0 14px; text-align:center; color:#65706b; font-size:12px;\">" + escapeHtml(notice) + "</p>";
        String previewMetaHtml = notice == null || notice.isBlank()
                ? ""
                : buildPreviewMeta(destinataire, date);

        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>").append(escapeHtml(subject)).append("</title>")
                .append("</head>")
                .append("<body style=\"margin:0; padding:0; background:#f4f1ea; color:#1d252b; font-family:Arial, Helvetica, sans-serif;\">")
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%; background:#f4f1ea; padding:28px 12px;\">")
                .append("<tr><td align=\"center\">")
                .append(noticeHtml)
                .append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%; max-width:640px; border-collapse:collapse; background:#fffdf9; border:1px solid #e4ded2; border-radius:12px; overflow:hidden; box-shadow:0 12px 28px rgba(29,37,43,0.08);\">")
                .append("<tr><td style=\"padding:26px 32px 14px; text-align:center; background:#fffdf9;\">")
                .append("<div style=\"font-size:18px; font-weight:800; letter-spacing:0.02em; color:#2f5a4f;\">")
                .append(escapeHtml(companyName))
                .append("</div>")
                .append("</td></tr>")
                .append("<tr><td style=\"padding:0 32px 22px; text-align:center; border-bottom:1px solid #e9e1d4;\">")
                .append("<h1 style=\"margin:0; color:#1d252b; font-size:24px; line-height:1.25; font-weight:800;\">")
                .append(escapeHtml(subject))
                .append("</h1>")
                .append(previewMetaHtml)
                .append("</td></tr>")
                .append("<tr><td style=\"padding:28px 32px 26px; font-size:15px; line-height:1.65; color:#1d252b;\">")
                .append(contentHtml != null ? contentHtml : "<p>Aucun contenu.</p>")
                .append("</td></tr>")
                .append("<tr><td style=\"padding:18px 32px 22px; background:#f9f6ef; border-top:1px solid #e9e1d4; color:#65706b; font-size:12px; line-height:1.6;\">")
                .append(buildEmailFooter(settings, companyName))
                .append("</td></tr>")
                .append("</table>")
                .append("</td></tr></table>")
                .append("</body></html>");
        return builder.toString();
    }

    private String buildPreviewMeta(String destinataire, LocalDateTime date) {
        StringBuilder builder = new StringBuilder("<div style=\"margin-top:12px; color:#65706b; font-size:13px; line-height:1.5;\">");
        boolean hasDestinataire = destinataire != null && !destinataire.isBlank();
        if (destinataire != null && !destinataire.isBlank()) {
            builder.append("<span>Destinataire : ")
                    .append(escapeHtml(destinataire))
                    .append("</span>");
        }
        if (date != null) {
            if (hasDestinataire) {
                builder.append("<br>");
            }
            builder.append("<span>Date : ")
                    .append(escapeHtml(date.format(DATETIME_FORMAT)))
                    .append("</span>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String buildEmailFooter(EmailSettingsDto settings, String companyName) {
        StringBuilder builder = new StringBuilder();
        builder.append("<strong style=\"color:#2f5a4f;\">")
                .append(escapeHtml(companyName))
                .append("</strong>");
        appendFooterLine(builder, settings.getAdresseEmail());
        appendFooterLine(builder, settings.getTelephone());
        appendFooterLine(builder, settings.getAdressePostale());
        appendFooterLine(builder, settings.getMentionsLegales());
        return builder.toString();
    }

    private void appendFooterLine(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append("<br>").append(escapeHtml(value));
        }
    }

    private EmailSettingsDto getSafeEmailSettings() {
        try {
            EmailSettingsDto settings = emailSettingsService.getSettings();
            if (settings != null) {
                return settings;
            }
        } catch (RuntimeException exception) {
            // L'email reste secondaire: un probleme de configuration ne doit pas bloquer le rendu.
        }

        return EmailSettingsDto.builder()
                .nomEntreprise("Terra Sana")
                .adresseEmail("contact@terrasana.test")
                .telephone("+32 470 00 00 00")
                .adressePostale("Rue du Marche 12, 1000 Bruxelles")
                .mentionsLegales("TVA BE0000000000")
                .build();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
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

    private GeneratedEmailDto toDto(GeneratedEmail entity) {
        return GeneratedEmailDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .destinataire(entity.getDestinataire())
                .sujet(entity.getSujet())
                .contenuHtml(entity.getContenuHtml())
                .createdAt(entity.getCreatedAt())
                .statutEnvoi(entity.getStatutEnvoi() != null ? entity.getStatutEnvoi() : EmailSendStatus.PENDING)
                .dateTentativeEnvoi(entity.getDateTentativeEnvoi())
                .dateEnvoi(entity.getDateEnvoi())
                .erreurEnvoi(entity.getErreurEnvoi())
                .commandeId(entity.getCommandeId())
                .membreId(entity.getMembreId())
                .produitId(entity.getProduitId())
                .demandeAdhesionId(entity.getDemandeAdhesionId())
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncateError(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private record EmailReferences(
            Long commandeId,
            Long membreId,
            Long produitId,
            Long demandeAdhesionId
    ) {
    }
}
