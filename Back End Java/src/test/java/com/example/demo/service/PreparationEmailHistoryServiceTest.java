package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.service.EmailSenderService.EmailSendResult;

@ExtendWith(MockitoExtension.class)
class PreparationEmailHistoryServiceTest {

    @Mock
    private GeneratedEmailRepository repository;

    @InjectMocks
    private PreparationEmailHistoryService service;

    @Test
    void automaticClaimCreatesOnePendingHistoryEntry() {
        when(repository.findByTypeAndCreneauCollecteId(
                EmailNotificationType.PREPARATION_DOSSIER,
                7L
        )).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(GeneratedEmail.class))).thenAnswer(invocation -> {
            GeneratedEmail email = invocation.getArgument(0);
            email.setId(41L);
            return email;
        });

        var result = service.claimAutomatic(7L, "prep@test", "Sujet", "<p>Corps</p>");

        assertThat(result.acquired()).isTrue();
        assertThat(result.emailId()).isEqualTo(41L);
        ArgumentCaptor<GeneratedEmail> captor = ArgumentCaptor.forClass(GeneratedEmail.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(EmailNotificationType.PREPARATION_DOSSIER);
        assertThat(captor.getValue().getCreneauCollecteId()).isEqualTo(7L);
        assertThat(captor.getValue().getStatutEnvoi()).isEqualTo(EmailSendStatus.PENDING);
    }

    @ParameterizedTest
    @EnumSource(EmailSendStatus.class)
    void automaticClaimNeverRetriesExistingPendingSentOrFailed(EmailSendStatus status) {
        GeneratedEmail existing = email(51L, status);
        when(repository.findByTypeAndCreneauCollecteId(
                EmailNotificationType.PREPARATION_DOSSIER,
                7L
        )).thenReturn(Optional.of(existing));

        var result = service.claimAutomatic(7L, "prep@test", "Sujet", "Corps");

        assertThat(result.acquired()).isFalse();
        assertThat(result.currentStatus()).isEqualTo(status);
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = EmailSendStatus.class, names = {"SENT", "FAILED"})
    void manualClaimResetsCompletedEntryToPending(EmailSendStatus previousStatus) {
        GeneratedEmail existing = email(61L, previousStatus);
        existing.setErreurEnvoi(previousStatus == EmailSendStatus.FAILED ? "Ancienne erreur" : null);
        existing.setDateTentativeEnvoi(LocalDateTime.of(2026, 10, 1, 8, 0));
        when(repository.findByTypeAndCreneauCollecteIdForUpdate(
                EmailNotificationType.PREPARATION_DOSSIER,
                7L
        )).thenReturn(Optional.of(existing));

        var result = service.claimManual(7L, "new@test", "Nouveau", "Nouveau corps");

        assertThat(result.acquired()).isTrue();
        assertThat(existing.getStatutEnvoi()).isEqualTo(EmailSendStatus.PENDING);
        assertThat(existing.getErreurEnvoi()).isNull();
        assertThat(existing.getDateTentativeEnvoi()).isNull();
        assertThat(existing.getDestinataire()).isEqualTo("new@test");
        verify(repository).save(existing);
    }

    @Test
    void manualClaimDoesNotStartSecondConcurrentPendingSend() {
        GeneratedEmail existing = email(71L, EmailSendStatus.PENDING);
        when(repository.findByTypeAndCreneauCollecteIdForUpdate(
                EmailNotificationType.PREPARATION_DOSSIER,
                7L
        )).thenReturn(Optional.of(existing));

        var result = service.claimManual(7L, "prep@test", "Sujet", "Corps");

        assertThat(result.acquired()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void completionStoresFailedResult() {
        GeneratedEmail existing = email(81L, EmailSendStatus.PENDING);
        when(repository.findByIdForUpdate(81L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.complete(81L, EmailSendResult.failed(
                LocalDateTime.of(2026, 10, 9, 14, 0),
                "SMTP indisponible"
        ));

        assertThat(result.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(result.getErreurEnvoi()).isEqualTo("SMTP indisponible");
        assertThat(existing.getDateEnvoi()).isNull();
    }

    @Test
    void completionStoresSentResult() {
        GeneratedEmail existing = email(91L, EmailSendStatus.PENDING);
        LocalDateTime attemptedAt = LocalDateTime.of(2026, 10, 9, 14, 0);
        LocalDateTime sentAt = attemptedAt.plusSeconds(1);
        when(repository.findByIdForUpdate(91L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.complete(91L, EmailSendResult.sent(attemptedAt, sentAt));

        assertThat(result.getStatutEnvoi()).isEqualTo(EmailSendStatus.SENT);
        assertThat(result.getDateEnvoi()).isEqualTo(sentAt);
        assertThat(result.getErreurEnvoi()).isNull();
    }

    private GeneratedEmail email(Long id, EmailSendStatus status) {
        return GeneratedEmail.builder()
                .id(id)
                .type(EmailNotificationType.PREPARATION_DOSSIER)
                .creneauCollecteId(7L)
                .destinataire("prep@test")
                .sujet("Sujet")
                .contenuHtml("Corps")
                .statutEnvoi(status)
                .build();
    }
}
