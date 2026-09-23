package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.ConfigurationEmailRepository;
import com.example.demo.dto.EmailSettingsDto;
import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.ConfigurationEmail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailSettingsService {

    private static final long CONFIG_ID = 1L;
    private final ConfigurationEmailRepository configurationEmailRepository;
    private final ObjectMapper objectMapper;
    private final EmailTemplateService emailTemplateService;

    @Transactional
    public void initializeDefaults() {
        emailTemplateService.ensureDefaultTemplates();
        if (configurationEmailRepository.existsById(CONFIG_ID)) {
            return;
        }

        ConfigurationEmail entity = ConfigurationEmail.builder()
                .id(CONFIG_ID)
                .payload(serialize(defaultSettings()))
                .build();
        configurationEmailRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public EmailSettingsDto getSettings() {
        emailTemplateService.ensureDefaultTemplates();
        EmailSettingsDto companySettings = configurationEmailRepository.findById(CONFIG_ID)
                .map(ConfigurationEmail::getPayload)
                .map(this::deserialize)
                .orElseGet(this::defaultSettings);

        return mergeTemplates(companySettings);
    }

    @Transactional
    public EmailSettingsDto saveSettings(EmailSettingsDto dto) {
        emailTemplateService.ensureDefaultTemplates();
        EmailSettingsDto sanitized = sanitize(dto);
        ConfigurationEmail entity = configurationEmailRepository.findById(CONFIG_ID)
                .orElseGet(() -> ConfigurationEmail.builder().id(CONFIG_ID).build());
        entity.setPayload(serialize(sanitized));
        configurationEmailRepository.save(entity);

        updateCoreTemplate(EmailNotificationType.ORDER_CONFIRMATION, dto != null ? dto.getConfirmationCommande() : null);
        updateCoreTemplate(EmailNotificationType.ORDER_REMINDER_J1, dto != null ? dto.getRappelJ1() : null);
        updateCoreTemplate(EmailNotificationType.ORDER_CANCELLATION, dto != null ? dto.getAnnulationCommande() : null);
        updateCoreTemplate(EmailNotificationType.MEMBERSHIP_VALIDATION, dto != null ? dto.getValidationAdhesion() : null);

        return getSettings();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, String> getCompanyVariables() {
        EmailSettingsDto settings = getSettings();
        java.util.Map<String, String> variables = new java.util.LinkedHashMap<>();
        variables.put("companyName", defaultIfBlank(settings.getNomEntreprise(), "Terra Sana"));
        variables.put("companyEmail", defaultIfBlank(settings.getAdresseEmail(), "contact@terrasana.test"));
        variables.put("companyPhone", defaultIfBlank(settings.getTelephone(), "+32 000 00 00 00"));
        variables.put("companyAddress", defaultIfBlank(settings.getAdressePostale(), "Rue du Marche 12, 1000 Bruxelles"));
        variables.put("legalMentions", defaultIfBlank(settings.getMentionsLegales(), "TVA BE0000000000"));
        return variables;
    }

    private EmailSettingsDto sanitize(EmailSettingsDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La configuration email est requise.");
        }

        return EmailSettingsDto.builder()
                .nomEntreprise(defaultIfBlank(dto.getNomEntreprise(), "Terra Sana"))
                .adresseEmail(defaultIfBlank(dto.getAdresseEmail(), "contact@terrasana.test"))
                .telephone(defaultIfBlank(dto.getTelephone(), "+32 000 00 00 00"))
                .adressePostale(defaultIfBlank(dto.getAdressePostale(), "Rue du Marche 12, 1000 Bruxelles"))
                .mentionsLegales(defaultIfBlank(dto.getMentionsLegales(), "TVA BE0000000000"))
                .confirmationCommande(sanitizeTemplate(dto.getConfirmationCommande(), emailTemplateService.getTemplate(EmailNotificationType.ORDER_CONFIRMATION)))
                .rappelJ1(sanitizeTemplate(dto.getRappelJ1(), emailTemplateService.getTemplate(EmailNotificationType.ORDER_REMINDER_J1)))
                .annulationCommande(sanitizeTemplate(dto.getAnnulationCommande(), emailTemplateService.getTemplate(EmailNotificationType.ORDER_CANCELLATION)))
                .validationAdhesion(sanitizeTemplate(dto.getValidationAdhesion(), emailTemplateService.getTemplate(EmailNotificationType.MEMBERSHIP_VALIDATION)))
                .build();
    }

    private EmailTemplateDto sanitizeTemplate(EmailTemplateDto dto, EmailTemplateDto defaults) {
        if (dto == null) {
            return defaults;
        }
        return EmailTemplateDto.builder()
                .type(defaults.getType())
                .objet(defaultIfBlank(dto.getObjet(), defaults.getObjet()))
                .contenu(defaultIfBlank(dto.getContenu(), defaults.getContenu()))
                .active(dto.getActive() != null ? dto.getActive() : defaults.getActive())
                .updatedAt(defaults.getUpdatedAt())
                .build();
    }

    private EmailSettingsDto defaultSettings() {
        return EmailSettingsDto.builder()
                .nomEntreprise("Terra Sana")
                .adresseEmail("contact@terrasana.test")
                .telephone("+32 470 00 00 00")
                .adressePostale("Rue du Marche 12, 1000 Bruxelles")
                .mentionsLegales("TVA BE0000000000")
                .confirmationCommande(emailTemplateService.getTemplate(EmailNotificationType.ORDER_CONFIRMATION))
                .rappelJ1(emailTemplateService.getTemplate(EmailNotificationType.ORDER_REMINDER_J1))
                .annulationCommande(emailTemplateService.getTemplate(EmailNotificationType.ORDER_CANCELLATION))
                .validationAdhesion(emailTemplateService.getTemplate(EmailNotificationType.MEMBERSHIP_VALIDATION))
                .build();
    }

    private EmailSettingsDto deserialize(String payload) {
        try {
            return sanitize(objectMapper.readValue(payload, EmailSettingsDto.class));
        } catch (Exception exception) {
            return defaultSettings();
        }
    }

    private String serialize(EmailSettingsDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de sauvegarder la configuration email.");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : fallback;
    }

    private EmailSettingsDto mergeTemplates(EmailSettingsDto settings) {
        settings.setConfirmationCommande(emailTemplateService.getTemplate(EmailNotificationType.ORDER_CONFIRMATION));
        settings.setRappelJ1(emailTemplateService.getTemplate(EmailNotificationType.ORDER_REMINDER_J1));
        settings.setAnnulationCommande(emailTemplateService.getTemplate(EmailNotificationType.ORDER_CANCELLATION));
        settings.setValidationAdhesion(emailTemplateService.getTemplate(EmailNotificationType.MEMBERSHIP_VALIDATION));
        return settings;
    }

    private void updateCoreTemplate(EmailNotificationType type, EmailTemplateDto dto) {
        if (dto == null) {
            return;
        }
        emailTemplateService.updateTemplate(type, dto);
    }
}
