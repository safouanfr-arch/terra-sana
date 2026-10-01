package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.EmailTemplateRepository;
import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.model.EmailTemplate;
import com.example.demo.model.enums.EmailNotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w]+)\\s*}}");

    private final EmailTemplateRepository emailTemplateRepository;

    @Transactional
    public void ensureDefaultTemplates() {
        defaultTemplates().forEach((type, template) -> emailTemplateRepository.findById(type)
                .orElseGet(() -> emailTemplateRepository.save(toEntity(type, template, true, null))));
    }

    @Transactional
    public List<EmailTemplateDto> getAllTemplates() {
        ensureDefaultTemplates();
        return emailTemplateRepository.findAllByOrderByTypeAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EmailTemplateDto getTemplate(EmailNotificationType type) {
        ensureDefaultTemplates();
        return toDto(resolveTemplate(type));
    }

    @Transactional
    public EmailTemplateDto updateTemplate(EmailNotificationType type, EmailTemplateDto dto) {
        EmailTemplate existing = resolveTemplate(type);
        existing.setSubjectTemplate(defaultIfBlank(dto.getObjet(), existing.getSubjectTemplate()));
        existing.setBodyTemplate(defaultIfBlank(dto.getContenu(), existing.getBodyTemplate()));
        existing.setActive(dto.getActive() != null ? dto.getActive() : existing.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return toDto(emailTemplateRepository.save(existing));
    }

    @Transactional
    public RenderedEmail renderTemplate(
            EmailNotificationType type,
            Map<String, String> variables,
            EmailTemplateDto overrideTemplate
    ) {
        ensureDefaultTemplates();
        EmailTemplate storedTemplate = resolveTemplate(type);
        EmailTemplateDto effectiveTemplate = EmailTemplateDto.builder()
                .type(type.name())
                .objet(defaultIfBlank(overrideTemplate != null ? overrideTemplate.getObjet() : null, storedTemplate.getSubjectTemplate()))
                .contenu(defaultIfBlank(overrideTemplate != null ? overrideTemplate.getContenu() : null, storedTemplate.getBodyTemplate()))
                .active(overrideTemplate != null && overrideTemplate.getActive() != null ? overrideTemplate.getActive() : storedTemplate.getActive())
                .updatedAt(storedTemplate.getUpdatedAt())
                .build();

        String renderedSubject = replaceVariables(effectiveTemplate.getObjet(), variables);
        String renderedBody = replaceVariables(effectiveTemplate.getContenu(), variables);

        return new RenderedEmail(effectiveTemplate, renderedSubject, renderedBody);
    }

    @Transactional
    public EmailTemplateDto getDefaultTemplateDto(EmailNotificationType type) {
        EmailTemplateDto dto = defaultTemplates().get(type);
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template email introuvable.");
        }
        return EmailTemplateDto.builder()
                .type(type.name())
                .objet(dto.getObjet())
                .contenu(dto.getContenu())
                .active(true)
                .build();
    }

    private EmailTemplate resolveTemplate(EmailNotificationType type) {
        return emailTemplateRepository.findById(type)
                .orElseGet(() -> emailTemplateRepository.save(toEntity(type, getDefaultTemplateDto(type), true, null)));
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String safeTemplate = template != null ? template : "";
        Map<String, String> safeVariables = variables != null ? variables : Map.of();

        Matcher matcher = VARIABLE_PATTERN.matcher(safeTemplate);
        StringBuffer rendered = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = safeVariables.getOrDefault(key, "");
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement != null ? replacement : ""));
        }

        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private EmailTemplate toEntity(
            EmailNotificationType type,
            EmailTemplateDto dto,
            boolean active,
            LocalDateTime updatedAt
    ) {
        return EmailTemplate.builder()
                .type(type)
                .subjectTemplate(dto.getObjet())
                .bodyTemplate(dto.getContenu())
                .active(active)
                .updatedAt(updatedAt != null ? updatedAt : LocalDateTime.now())
                .build();
    }

    private EmailTemplateDto toDto(EmailTemplate entity) {
        return EmailTemplateDto.builder()
                .type(entity.getType().name())
                .objet(entity.getSubjectTemplate())
                .contenu(entity.getBodyTemplate())
                .active(entity.getActive())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private Map<EmailNotificationType, EmailTemplateDto> defaultTemplates() {
        Map<EmailNotificationType, EmailTemplateDto> defaults = new EnumMap<>(EmailNotificationType.class);
        defaults.put(EmailNotificationType.ORDER_CONFIRMATION, EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_CONFIRMATION.name())
                .objet("Confirmation de votre commande {{numero}}")
                .contenu("""
                        Bonjour {{prenom}},

                        Votre commande {{numero}} est bien confirmée.

                        Point de collecte : {{pointCollecte}}
                        Date de retrait : {{dateRetrait}}
                        Créneau : {{creneauRetrait}}
                        Montant : {{montant}}
                        Code de retrait : {{codeRetrait}}

                        {{detailProduits}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.ORDER_REMINDER_J1, EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_REMINDER_J1.name())
                .objet("Rappel de retrait pour votre commande {{numero}}")
                .contenu("""
                        Bonjour {{prenom}},

                        Petit rappel : votre commande {{numero}} est à retirer demain.

                        Point de collecte : {{pointCollecte}}
                        Date de retrait : {{dateRetrait}}
                        Créneau : {{creneauRetrait}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.ORDER_CANCELLATION, EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_CANCELLATION.name())
                .objet("Annulation de votre commande {{numero}}")
                .contenu("""
                        Bonjour {{prenom}},

                        Votre commande {{numero}} a été annulée.
                        Si besoin, nous restons disponibles à {{companyEmail}}.
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.MEMBERSHIP_VALIDATION, EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_VALIDATION.name())
                .objet("Votre adhésion Terra Sana est validée")
                .contenu("""
                        Bonjour {{prenom}},

                        Votre demande d'adhésion Terra Sana a été acceptée.
                        Vous pouvez définir votre mot de passe ici :
                        {{lienMotDePasse}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.MEMBERSHIP_REJECTION, EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_REJECTION.name())
                .objet("Suite à votre demande d'adhésion Terra Sana")
                .contenu("""
                        Bonjour {{prenom}},

                        Votre demande d'adhésion n'a pas pu être acceptée.
                        Motif : {{motifRefus}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.MEMBERSHIP_REQUEST_RECEIVED, EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_REQUEST_RECEIVED.name())
                .objet("Votre demande d'adhésion Terra Sana a bien été reçue")
                .contenu("""
                        Bonjour {{prenom}},

                        Nous avons bien reçu votre demande d'adhésion.
                        Notre équipe reviendra vers vous après vérification.
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.MEMBERSHIP_EMAIL_CONFIRMATION, EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_EMAIL_CONFIRMATION.name())
                .objet("Terra Sana - Confirmez votre adresse email")
                .contenu("""
                        Bonjour {{prenom}},

                        Confirmez votre adresse email afin de transmettre votre demande d'adhésion à Terra Sana :
                        {{lienConfirmation}}

                        Ce lien est valable pendant 24 heures. Sans confirmation, votre demande ne sera pas transmise à notre équipe.
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.ORDER_READY, EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_READY.name())
                .objet("Votre commande {{numero}} est prête")
                .contenu("""
                        Bonjour {{prenom}},

                        Votre commande {{numero}} est prête à être retirée.

                        Point de collecte : {{pointCollecte}}
                        Date de retrait : {{dateRetrait}}
                        Créneau : {{creneauRetrait}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.LOW_STOCK_ALERT, EmailTemplateDto.builder()
                .type(EmailNotificationType.LOW_STOCK_ALERT.name())
                .objet("Alerte stock faible - {{produitNom}}")
                .contenu("""
                        Le stock du produit {{produitNom}} est passé sous le seuil.

                        Stock actuel : {{stockActuel}}
                        Seuil de vigilance : {{seuilStock}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.OUT_OF_STOCK_ALERT, EmailTemplateDto.builder()
                .type(EmailNotificationType.OUT_OF_STOCK_ALERT.name())
                .objet("Rupture de stock - {{produitNom}}")
                .contenu("""
                        Le produit {{produitNom}} est maintenant en rupture de stock.

                        Stock actuel : {{stockActuel}}
                        Seuil de vigilance : {{seuilStock}}
                        """)
                .active(true)
                .build());
        defaults.put(EmailNotificationType.PASSWORD_SETUP, EmailTemplateDto.builder()
                .type(EmailNotificationType.PASSWORD_SETUP.name())
                .objet("Lien pour définir ou réinitialiser votre mot de passe")
                .contenu("""
                        Bonjour {{prenom}},

                        Vous pouvez définir ou réinitialiser votre mot de passe via le lien suivant :
                        {{lienMotDePasse}}
                        """)
                .active(true)
                .build());
        return defaults;
    }

    public record RenderedEmail(
            EmailTemplateDto template,
            String subject,
            String body
    ) {
    }
}
