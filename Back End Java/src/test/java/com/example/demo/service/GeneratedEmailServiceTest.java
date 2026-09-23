package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.dto.EmailSettingsDto;
import com.example.demo.dto.EmailTemplateDto;
import com.example.demo.dto.GeneratedEmailDto;
import com.example.demo.model.Categorie;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.UniteProduit;
import com.example.demo.service.EmailSenderService.EmailSendResult;

@ExtendWith(MockitoExtension.class)
public class GeneratedEmailServiceTest {

    @Mock
    private GeneratedEmailRepository generatedEmailRepository;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private EmailSettingsService emailSettingsService;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private AdministrateurRepository administrateurRepository;

    @Mock
    private CommandeRepository commandeRepository;

    @InjectMocks
    private GeneratedEmailService generatedEmailService;

    @Test
    void previewGeneratedEmailReturnsHtml() {
        GeneratedEmail entity = GeneratedEmail.builder()
                .id(7L)
                .type(EmailNotificationType.ORDER_CONFIRMATION)
                .destinataire("membre@terra.test")
                .sujet("Confirmation")
                .contenuHtml("<p>Bonjour</p>")
                .createdAt(LocalDateTime.of(2026, 4, 26, 18, 0))
                .build();

        when(generatedEmailRepository.findById(7L)).thenReturn(Optional.of(entity));
        when(emailSettingsService.getSettings()).thenReturn(EmailSettingsDto.builder()
                .nomEntreprise("Terra Sana")
                .adresseEmail("contact@terrasana.test")
                .telephone("+32 470 00 00 00")
                .adressePostale("Rue du Marche 12")
                .mentionsLegales("TVA BE0000000000")
                .build());

        String html = generatedEmailService.previewGeneratedEmail(7L);

        assertThat(html).contains("Apercu de l&#39;email enregistre");
        assertThat(html).contains("Confirmation");
        assertThat(html).contains("membre@terra.test");
    }

    @Test
    void generateOrderConfirmationStoresSentStatusWhenSmtpSucceeds() {
        Commande commande = buildCommande();
        EmailTemplateDto template = EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_CONFIRMATION.name())
                .active(true)
                .build();

        when(emailSettingsService.getCompanyVariables()).thenReturn(Map.of(
                "companyName", "Terra Sana",
                "companyEmail", "contact@terrasana.test"
        ));
        when(emailTemplateService.renderTemplate(eq(EmailNotificationType.ORDER_CONFIRMATION), any(), eq(null)))
                .thenReturn(new EmailTemplateService.RenderedEmail(
                        template,
                        "Confirmation CMD-1",
                        "Bonjour Marie, commande CMD-1"
                ));
        when(generatedEmailRepository.save(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            if (email.getId() == null) {
                email.setId(12L);
            }
            return email;
        });
        when(emailSenderService.sendHtmlEmail(
                eq("membre@terra.test"),
                eq("Confirmation CMD-1"),
                contains("Bonjour Marie")
        )).thenReturn(EmailSendResult.sent(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 10, 0, 2)
        ));

        var dto = generatedEmailService.generateOrderConfirmation(commande);

        assertThat(dto.getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        assertThat(dto.getDateTentativeEnvoi()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0));
        assertThat(dto.getDateEnvoi()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 0, 2));
        assertThat(dto.getErreurEnvoi()).isNull();
        assertThat(dto.getDestinataire()).isEqualTo("membre@terra.test");
        verify(emailSenderService).sendHtmlEmail(
                eq("membre@terra.test"),
                eq("Confirmation CMD-1"),
                contains("Bonjour Marie")
        );
    }

    @Test
    void generateOrderConfirmationStoresFailedStatusWhenSmtpFails() {
        Commande commande = buildCommande();
        EmailTemplateDto template = EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_CONFIRMATION.name())
                .active(true)
                .build();

        when(emailSettingsService.getCompanyVariables()).thenReturn(Map.of());
        when(emailTemplateService.renderTemplate(eq(EmailNotificationType.ORDER_CONFIRMATION), any(), eq(null)))
                .thenReturn(new EmailTemplateService.RenderedEmail(
                        template,
                        "Confirmation CMD-1",
                        "Bonjour Marie"
                ));
        when(generatedEmailRepository.save(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            if (email.getId() == null) {
                email.setId(13L);
            }
            return email;
        });
        when(emailSenderService.sendHtmlEmail(any(), any(), any()))
                .thenReturn(EmailSendResult.failed(
                        LocalDateTime.of(2026, 5, 1, 10, 5),
                        "SMTP indisponible"
                ));

        var dto = generatedEmailService.generateOrderConfirmation(commande);

        assertThat(dto.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(dto.getDateTentativeEnvoi()).isEqualTo(LocalDateTime.of(2026, 5, 1, 10, 5));
        assertThat(dto.getDateEnvoi()).isNull();
        assertThat(dto.getErreurEnvoi()).contains("SMTP indisponible");
    }

    @Test
    void generateMembershipValidationRendersPasswordLinkAndSendsToCandidate() {
        DemandeAdhesion demande = DemandeAdhesion.builder()
                .id(14L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .build();
        String passwordLink = "http://localhost:5173/creation-mot-de-passe/token-demo";
        EmailTemplateDto template = EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_VALIDATION.name())
                .active(true)
                .build();

        when(emailSettingsService.getCompanyVariables()).thenReturn(Map.of("companyName", "Terra Sana"));
        when(emailTemplateService.renderTemplate(eq(EmailNotificationType.MEMBERSHIP_VALIDATION), any(), eq(null)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> variables = invocation.getArgument(1);
                    return new EmailTemplateService.RenderedEmail(
                            template,
                            "Votre adhesion Terra Sana est validee",
                            "Bonjour " + variables.get("prenom") + ",\nDefinissez votre mot de passe ici :\n"
                                    + variables.get("lienMotDePasse")
                    );
                });
        when(generatedEmailRepository.save(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            if (email.getId() == null) {
                email.setId(15L);
            }
            return email;
        });
        when(emailSenderService.sendHtmlEmail(
                eq("marie@terra.test"),
                eq("Votre adhesion Terra Sana est validee"),
                contains(passwordLink)
        )).thenReturn(EmailSendResult.sent(
                LocalDateTime.of(2026, 5, 1, 11, 0),
                LocalDateTime.of(2026, 5, 1, 11, 0, 1)
        ));

        var dto = generatedEmailService.generateMembershipValidation(demande, passwordLink);

        assertThat(dto.getDestinataire()).isEqualTo("marie@terra.test");
        assertThat(dto.getType()).isEqualTo(EmailNotificationType.MEMBERSHIP_VALIDATION);
        assertThat(dto.getContenuHtml()).contains(passwordLink);
        assertThat(dto.getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        verify(emailSenderService).sendHtmlEmail(
                eq("marie@terra.test"),
                eq("Votre adhesion Terra Sana est validee"),
                contains(passwordLink)
        );
    }

    @Test
    void generateMembershipValidationKeepsPasswordLinkWhenTemplateOmitsVariable() {
        DemandeAdhesion demande = DemandeAdhesion.builder()
                .id(16L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .build();
        String passwordLink = "http://localhost:5173/creation-mot-de-passe/token-demo";
        EmailTemplateDto template = EmailTemplateDto.builder()
                .type(EmailNotificationType.MEMBERSHIP_VALIDATION.name())
                .active(true)
                .build();

        when(emailSettingsService.getCompanyVariables()).thenReturn(Map.of("companyName", "Terra Sana"));
        when(emailTemplateService.renderTemplate(eq(EmailNotificationType.MEMBERSHIP_VALIDATION), any(), eq(null)))
                .thenReturn(new EmailTemplateService.RenderedEmail(
                        template,
                        "Votre adhesion Terra Sana est validee",
                        "Bonjour Marie, votre adhesion est validee."
                ));
        when(generatedEmailRepository.save(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            if (email.getId() == null) {
                email.setId(17L);
            }
            return email;
        });
        when(emailSenderService.sendHtmlEmail(
                eq("marie@terra.test"),
                eq("Votre adhesion Terra Sana est validee"),
                contains(passwordLink)
        )).thenReturn(EmailSendResult.sent(
                LocalDateTime.of(2026, 5, 1, 11, 15),
                LocalDateTime.of(2026, 5, 1, 11, 15, 1)
        ));

        var dto = generatedEmailService.generateMembershipValidation(demande, passwordLink);

        assertThat(dto.getContenuHtml()).contains(passwordLink);
        assertThat(dto.getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        verify(emailSenderService).sendHtmlEmail(
                eq("marie@terra.test"),
                eq("Votre adhesion Terra Sana est validee"),
                contains(passwordLink)
        );
    }

    @Test
    void automaticReminderGeneratesEmailWhenWithdrawalIsTomorrow() {
        Commande commande = buildCommande();
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));
        when(generatedEmailRepository.existsByTypeAndCommandeId(
                EmailNotificationType.ORDER_REMINDER_J1,
                commande.getId()
        )).thenReturn(false);
        stubReminderEmail(EmailSendResult.sent(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 10, 0, 1)
        ));

        Optional<GeneratedEmailDto> result = generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        verify(emailSenderService).sendHtmlEmail(any(), any(), any());
    }

    @Test
    void automaticReminderIgnoresWithdrawalTodayOrAfterTomorrow() {
        Commande commande = buildCommande();
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));

        commande.setDateRetraitPrevu(LocalDateTime.of(2026, 5, 1, 18, 0));
        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isEmpty();

        commande.setDateRetraitPrevu(LocalDateTime.of(2026, 5, 3, 18, 0));
        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isEmpty();

        verifyNoInteractions(emailSenderService);
    }

    @Test
    void automaticReminderIgnoresCancelledOrder() {
        Commande commande = buildCommande();
        commande.setStatut(StatutCommande.CANCELLED);
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));

        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isEmpty();

        verifyNoInteractions(emailSenderService);
    }

    @Test
    void automaticReminderDoesNotDuplicateExistingHistory() {
        Commande commande = buildCommande();
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));
        when(generatedEmailRepository.existsByTypeAndCommandeId(
                EmailNotificationType.ORDER_REMINDER_J1,
                commande.getId()
        )).thenReturn(true);

        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isEmpty();

        verifyNoInteractions(emailSenderService);
    }

    @Test
    void automaticReminderStoresFailedEmailWithoutChangingOrderWhenSmtpFails() {
        Commande commande = buildCommande();
        StatutCommande initialStatus = commande.getStatut();
        LocalDateTime initialWithdrawal = commande.getDateRetraitPrevu();
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));
        when(generatedEmailRepository.existsByTypeAndCommandeId(
                EmailNotificationType.ORDER_REMINDER_J1,
                commande.getId()
        )).thenReturn(false);
        stubReminderEmail(EmailSendResult.failed(
                LocalDateTime.of(2026, 5, 1, 10, 5),
                "SMTP indisponible"
        ));

        var result = generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(result.orElseThrow().getErreurEnvoi()).contains("SMTP indisponible");
        assertThat(commande.getStatut()).isEqualTo(initialStatus);
        assertThat(commande.getDateRetraitPrevu()).isEqualTo(initialWithdrawal);
        verify(commandeRepository, never()).save(any(Commande.class));
    }

    @Test
    void repeatedAutomaticReminderCallsSendOnlyOnce() {
        Commande commande = buildCommande();
        when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));
        when(generatedEmailRepository.existsByTypeAndCommandeId(
                EmailNotificationType.ORDER_REMINDER_J1,
                commande.getId()
        )).thenReturn(false, true);
        stubReminderEmail(EmailSendResult.sent(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 10, 0, 1)
        ));

        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isPresent();
        assertThat(generatedEmailService.sendOrderReminderIfDue(
                commande.getId(),
                LocalDate.of(2026, 5, 2)
        )).isEmpty();

        verify(emailSenderService, times(1)).sendHtmlEmail(any(), any(), any());
    }

    private void stubReminderEmail(EmailSendResult sendResult) {
        EmailTemplateDto template = EmailTemplateDto.builder()
                .type(EmailNotificationType.ORDER_REMINDER_J1.name())
                .active(true)
                .build();
        when(emailSettingsService.getCompanyVariables()).thenReturn(Map.of("companyName", "Terra Sana"));
        when(emailTemplateService.renderTemplate(eq(EmailNotificationType.ORDER_REMINDER_J1), any(), eq(null)))
                .thenReturn(new EmailTemplateService.RenderedEmail(
                        template,
                        "Rappel de retrait CMD-1",
                        "Votre commande sera disponible demain."
                ));
        when(generatedEmailRepository.save(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            if (email.getId() == null) {
                email.setId(18L);
            }
            return email;
        });
        when(emailSenderService.sendHtmlEmail(any(), any(), any())).thenReturn(sendResult);
    }

    private Commande buildCommande() {
        Membre membre = Membre.builder()
                .id(4L)
                .nom("Dupont")
                .prenom("Marie")
                .email("membre@terra.test")
                .actif(true)
                .build();
        Produit produit = Produit.builder()
                .Id(8L)
                .nom("Carottes bio")
                .unite(UniteProduit.KG)
                .categorie(Categorie.builder().id(2L).nom("Legumes").build())
                .build();
        Commande commande = Commande.builder()
                .id(9L)
                .numero("CMD-1")
                .codeRetrait("ABC123")
                .membre(membre)
                .montantTTC(new BigDecimal("12.50"))
                .dateRetraitPrevu(LocalDateTime.of(2026, 5, 2, 18, 0))
                .creneauCollecte(CreneauCollecte.builder()
                        .id(7L)
                        .creneau(LocalDate.of(2026, 5, 2))
                        .heureDebut(LocalTime.of(18, 0))
                        .heureFin(LocalTime.of(20, 0))
                        .pointCollecte(PointCollecte.builder().id(1L).nom("Depot").build())
                        .build())
                .build();
        LigneCommande ligne = LigneCommande.builder()
                .commande(commande)
                .produit(produit)
                .quantite(new BigDecimal("2.00"))
                .montantTTC(new BigDecimal("12.50"))
                .build();
        commande.setLignes(List.of(ligne));
        return commande;
    }
}
