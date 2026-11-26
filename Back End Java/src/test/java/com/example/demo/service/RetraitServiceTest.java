package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dto.CommandeCreneauResumeDto;
import com.example.demo.dto.CommandeMembreResumeDto;
import com.example.demo.dto.CommandePointCollecteResumeDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.dto.RetraitCodeRequestDto;
import com.example.demo.dto.RetraitCommandeDto;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.Membre;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.enums.StatutCommande;

@ExtendWith(MockitoExtension.class)
class RetraitServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private CommandeService commandeService;

    @InjectMocks
    private RetraitService retraitService;

    @Test
    void searchFindsReadyOrderAndNormalizesCode() {
        Commande commande = readyCommande();
        when(commandeRepository.findByCodeRetrait("ABC123")).thenReturn(Optional.of(commande));

        RetraitCommandeDto result = retraitService.search(new RetraitCodeRequestDto("  abc123  "));

        assertThat(result.getCommandeId()).isEqualTo(9L);
        assertThat(result.getNumeroCommande()).isEqualTo("PC01-20400510-000009");
        assertThat(result.getMembrePrenom()).isEqualTo("Marie");
        assertThat(result.getMembreNom()).isEqualTo("Dupont");
        assertThat(result.getPointCollecte()).isEqualTo("Uccle");
        assertThat(result.getDateRetrait()).isEqualTo(LocalDate.of(2040, 5, 10));
        assertThat(result.getHeureDebut()).isEqualTo(LocalTime.of(14, 0));
        assertThat(result.getHeureFin()).isEqualTo(LocalTime.of(16, 0));
        assertThat(result.getMontantTotal()).isEqualByComparingTo("24.50");
        assertThat(result.getStatut()).isEqualTo(StatutCommande.READY);
        assertThat(result.getPaye()).isFalse();
        assertThat(result.getCodeRetrait()).isEqualTo("ABC123");
        verify(commandeRepository).findByCodeRetrait("ABC123");
    }

    @Test
    void searchRejectsUnknownCode() {
        when(commandeRepository.findByCodeRetrait("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retraitService.search(new RetraitCodeRequestDto("unknown")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void confirmDelegatesNormalizedCodeAndReturnsLimitedView() {
        LocalDateTime paymentDate = LocalDateTime.of(2040, 5, 10, 14, 5);
        CommandeResponseDto response = CommandeResponseDto.builder()
                .id(9L)
                .numero("PC01-20400510-000009")
                .codeRetrait("ABC123")
                .statut(StatutCommande.DISTRIBUTED)
                .paye(true)
                .datePaiement(paymentDate)
                .dateRetrait(LocalDateTime.of(2040, 5, 10, 14, 0))
                .montantTTC(new BigDecimal("24.50"))
                .membre(CommandeMembreResumeDto.builder().nom("Dupont").prenom("Marie").build())
                .pointCollecte(CommandePointCollecteResumeDto.builder().nom("Uccle").build())
                .creneauRetrait(CommandeCreneauResumeDto.builder()
                        .heureDebut(LocalTime.of(14, 0))
                        .heureFin(LocalTime.of(16, 0))
                        .build())
                .build();
        when(commandeService.confirmWithdrawalByCode("ABC123")).thenReturn(response);

        RetraitCommandeDto result = retraitService.confirm(new RetraitCodeRequestDto(" abc123 "));

        assertThat(result.getStatut()).isEqualTo(StatutCommande.DISTRIBUTED);
        assertThat(result.getPaye()).isTrue();
        assertThat(result.getDatePaiement()).isEqualTo(paymentDate);
        verify(commandeService).confirmWithdrawalByCode("ABC123");
    }

    private Commande readyCommande() {
        PointCollecte point = PointCollecte.builder().id(1L).nom("Uccle").build();
        CreneauCollecte creneau = CreneauCollecte.builder()
                .id(2L)
                .creneau(LocalDate.of(2040, 5, 10))
                .heureDebut(LocalTime.of(14, 0))
                .heureFin(LocalTime.of(16, 0))
                .pointCollecte(point)
                .build();
        return Commande.builder()
                .id(9L)
                .numero("PC01-20400510-000009")
                .codeRetrait("ABC123")
                .statut(StatutCommande.READY)
                .paye(false)
                .dateRetraitPrevu(LocalDateTime.of(2040, 5, 10, 14, 0))
                .montantTTC(new BigDecimal("24.50"))
                .membre(Membre.builder().nom("Dupont").prenom("Marie").build())
                .creneauCollecte(creneau)
                .build();
    }
}
