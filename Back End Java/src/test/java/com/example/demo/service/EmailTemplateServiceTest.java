package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.EmailTemplateRepository;
import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.model.EmailTemplate;
import com.example.demo.model.enums.EmailNotificationType;

@ExtendWith(MockitoExtension.class)
public class EmailTemplateServiceTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    @Test
    void renderTemplateReplacesKnownVariablesAndBlanksMissingOnes() {
        EmailTemplate template = EmailTemplate.builder()
                .type(EmailNotificationType.ORDER_CONFIRMATION)
                .subjectTemplate("Commande {{numero}} pour {{prenom}}")
                .bodyTemplate("Bonjour {{prenom}}, montant: {{montant}}")
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build();

        when(emailTemplateRepository.findById(EmailNotificationType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(template));

        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTemplate(
                EmailNotificationType.ORDER_CONFIRMATION,
                Map.of("numero", "CMD-1", "prenom", "Marie"),
                null
        );

        assertThat(rendered.subject()).isEqualTo("Commande CMD-1 pour Marie");
        assertThat(rendered.body()).isEqualTo("Bonjour Marie, montant: ");
    }

    @Test
    void renderTemplateUsesPreviewOverrideWhenProvided() {
        EmailTemplate template = EmailTemplate.builder()
                .type(EmailNotificationType.ORDER_CONFIRMATION)
                .subjectTemplate("Sujet stocke")
                .bodyTemplate("Contenu stocke")
                .active(true)
                .updatedAt(LocalDateTime.now())
                .build();

        when(emailTemplateRepository.findById(EmailNotificationType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(template));

        EmailTemplateService.RenderedEmail rendered = emailTemplateService.renderTemplate(
                EmailNotificationType.ORDER_CONFIRMATION,
                Map.of("prenom", "Marie"),
                EmailTemplateDto.builder()
                        .objet("Sujet test {{prenom}}")
                        .contenu("Bonjour {{prenom}}")
                        .active(true)
                        .build()
        );

        assertThat(rendered.subject()).isEqualTo("Sujet test Marie");
        assertThat(rendered.body()).isEqualTo("Bonjour Marie");
    }
}
