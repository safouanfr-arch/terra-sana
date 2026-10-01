package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.config.OrderPreparationEmailProperties;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dto.PreparationEmailStatusDto;
import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.EmailSenderService.EmailSendResult;
import com.example.demo.service.PreparationEmailHistoryService.ClaimResult;

@ExtendWith(MockitoExtension.class)
class PreparationEmailServiceTest {

    @Mock
    private CreneauCollecteRepository creneauCollecteRepository;
    @Mock
    private CommandeRepository commandeRepository;
    @Mock
    private PreparationGroupService preparationGroupService;
    @Mock
    private PreparationEmailHistoryService historyService;
    @Mock
    private EmailSenderService emailSenderService;

    private OrderPreparationEmailProperties properties;
    private PreparationEmailService service;
    private CreneauCollecte creneau;

    @BeforeEach
    void setUp() {
        properties = new OrderPreparationEmailProperties();
        properties.setEnabled(true);
        properties.setRecipient("preparation@terrasana.test");
        service = new PreparationEmailService(
                properties,
                creneauCollecteRepository,
                commandeRepository,
                preparationGroupService,
                historyService,
                emailSenderService
        );
        creneau = CreneauCollecte.builder()
                .id(7L)
                .creneau(LocalDate.of(2026, 10, 10))
                .heureDebut(LocalTime.of(14, 0))
                .heureFin(LocalTime.of(16, 0))
                .pointCollecte(PointCollecte.builder().nom("Bruxelles Centre").build())
                .build();
    }

    @Test
    void automaticSendGeneratesCurrentPdfAndCompletesHistoryAsSent() {
        stubEligibleSlot();
        byte[] pdf = "%PDF-current".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(historyService.claimAutomatic(eq(7L), eq("preparation@terrasana.test"), any(), any()))
                .thenReturn(ClaimResult.acquired(30L));
        when(preparationGroupService.generatePdf(7L))
                .thenReturn(new PreparationPdfDocument("preparation_Bruxelles_2026-10-10_14h00.pdf", pdf));
        EmailSendResult sent = EmailSendResult.sent(
                LocalDateTime.of(2026, 10, 9, 14, 0),
                LocalDateTime.of(2026, 10, 9, 14, 0, 1)
        );
        when(emailSenderService.sendHtmlEmailWithPdf(
                eq("preparation@terrasana.test"),
                contains("Bruxelles Centre - 10/10/2026 14:00"),
                contains("Nombre de commandes :</strong> 2"),
                eq("preparation_Bruxelles_2026-10-10_14h00.pdf"),
                eq(pdf)
        )).thenReturn(sent);
        when(historyService.complete(30L, sent)).thenReturn(status(EmailSendStatus.SENT));

        Optional<PreparationEmailStatusDto> result = service.sendAutomatically(7L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        assertThat(result.orElseThrow().isEnvoiConfigure()).isTrue();
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                historyService,
                preparationGroupService,
                emailSenderService
        );
        order.verify(historyService).claimAutomatic(eq(7L), any(), any(), any());
        order.verify(preparationGroupService).generatePdf(7L);
        order.verify(emailSenderService).sendHtmlEmailWithPdf(any(), any(), any(), any(), eq(pdf));
        order.verify(historyService).complete(30L, sent);
    }

    @Test
    void smtpFailureIsStoredAsFailedAfterPdfGeneration() {
        stubEligibleSlot();
        byte[] pdf = "%PDF-current".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(historyService.claimAutomatic(eq(7L), any(), any(), any()))
                .thenReturn(ClaimResult.acquired(31L));
        when(preparationGroupService.generatePdf(7L))
                .thenReturn(new PreparationPdfDocument("preparation.pdf", pdf));
        EmailSendResult failed = EmailSendResult.failed(
                LocalDateTime.of(2026, 10, 9, 14, 0),
                "SMTP indisponible"
        );
        when(emailSenderService.sendHtmlEmailWithPdf(any(), any(), any(), any(), eq(pdf)))
                .thenReturn(failed);
        when(historyService.complete(31L, failed)).thenReturn(status(EmailSendStatus.FAILED));

        PreparationEmailStatusDto result = service.sendAutomatically(7L).orElseThrow();

        assertThat(result.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
        verify(preparationGroupService).generatePdf(7L);
        verify(historyService).complete(31L, failed);
    }

    @Test
    void disabledConfigurationSkipsAutomaticEmailWithoutCrash() {
        properties.setEnabled(false);

        assertThat(service.sendAutomatically(7L)).isEmpty();

        verifyNoInteractions(creneauCollecteRepository, commandeRepository, historyService, emailSenderService);
    }

    @Test
    void missingRecipientSkipsAutomaticEmailWithoutCrash() {
        properties.setRecipient("  ");

        assertThat(service.sendAutomatically(7L)).isEmpty();

        verifyNoInteractions(creneauCollecteRepository, commandeRepository, historyService, emailSenderService);
    }

    @Test
    void noRelevantOrderProducesNoEmail() {
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(commandeRepository.countByCreneauCollecte_IdAndStatutIn(
                eq(7L),
                eq(java.util.List.of(StatutCommande.IN_PREP, StatutCommande.READY))
        )).thenReturn(0L);

        assertThat(service.sendAutomatically(7L)).isEmpty();

        verify(historyService, never()).claimAutomatic(anyLong(), any(), any(), any());
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void existingPendingHistoryPreventsConcurrentManualSend() {
        stubEligibleSlot();
        when(historyService.claimManual(eq(7L), any(), any(), any()))
                .thenReturn(ClaimResult.notAcquired(32L, EmailSendStatus.PENDING));
        when(historyService.findStatus(7L)).thenReturn(Optional.of(status(EmailSendStatus.PENDING)));

        PreparationEmailStatusDto result = service.sendManually(7L);

        assertThat(result.getStatutEnvoi()).isEqualTo(EmailSendStatus.PENDING);
        verifyNoInteractions(preparationGroupService, emailSenderService);
    }

    @Test
    void manualRetryRegeneratesPdfAndDoesNotReceiveRecipientFromCaller() {
        stubEligibleSlot();
        byte[] currentPdf = "%PDF-updated".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(historyService.claimManual(eq(7L), eq("preparation@terrasana.test"), any(), any()))
                .thenReturn(ClaimResult.acquired(33L));
        when(preparationGroupService.generatePdf(7L))
                .thenReturn(new PreparationPdfDocument("preparation_updated.pdf", currentPdf));
        EmailSendResult sent = EmailSendResult.sent(LocalDateTime.now(), LocalDateTime.now());
        when(emailSenderService.sendHtmlEmailWithPdf(
                eq("preparation@terrasana.test"),
                any(),
                any(),
                eq("preparation_updated.pdf"),
                eq(currentPdf)
        )).thenReturn(sent);
        when(historyService.complete(33L, sent)).thenReturn(status(EmailSendStatus.SENT));

        PreparationEmailStatusDto result = service.sendManually(7L);

        assertThat(result.getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        verify(preparationGroupService).generatePdf(7L);
    }

    @Test
    void manualSendRejectsMissingConfigurationClearly() {
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(commandeRepository.countByCreneauCollecte_IdAndStatutIn(anyLong(), any())).thenReturn(2L);
        properties.setRecipient("");

        assertThatThrownBy(() -> service.sendManually(7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    private void stubEligibleSlot() {
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(commandeRepository.countByCreneauCollecte_IdAndStatutIn(
                eq(7L),
                eq(java.util.List.of(StatutCommande.IN_PREP, StatutCommande.READY))
        )).thenReturn(2L);
    }

    private PreparationEmailStatusDto status(EmailSendStatus status) {
        return PreparationEmailStatusDto.builder()
                .creneauId(7L)
                .statutEnvoi(status)
                .erreurEnvoi(status == EmailSendStatus.FAILED ? "SMTP indisponible" : null)
                .build();
    }
}
