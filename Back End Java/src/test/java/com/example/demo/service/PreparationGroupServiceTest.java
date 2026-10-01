package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.LigneCommandeRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.PreparationGroupFinalizationDto;
import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.enums.StatutCommande;

@ExtendWith(MockitoExtension.class)
class PreparationGroupServiceTest {

    @Mock
    private CommandeRepository commandeRepository;
    @Mock
    private LigneCommandeRepository ligneCommandeRepository;
    @Mock
    private MouvementStockRepository mouvementStockRepository;
    @Mock
    private MembreRepository membreRepository;
    @Mock
    private CreneauCollecteRepository creneauCollecteRepository;
    @Mock
    private ProduitRepository produitRepository;
    @Mock
    private GeneratedEmailService generatedEmailService;
    @Mock
    private PromotionService promotionService;
    @Mock
    private PreparationPdfService preparationPdfService;

    private CommandeService commandeService;
    private PreparationGroupService preparationGroupService;

    @BeforeEach
    void setUp() {
        commandeService = new CommandeService(
                commandeRepository,
                ligneCommandeRepository,
                mouvementStockRepository,
                membreRepository,
                creneauCollecteRepository,
                produitRepository,
                generatedEmailService,
                promotionService
        );
        preparationGroupService = new PreparationGroupService(
                creneauCollecteRepository,
                commandeRepository,
                commandeService,
                preparationPdfService
        );
    }

    @Test
    void finalizesEveryEligibleOrderAndGeneratesOneReadyEmailPerOrder() {
        Commande first = commande(11L, StatutCommande.IN_PREP, false);
        Commande second = commande(12L, StatutCommande.IN_PREP, false);
        stubGroup(7L, List.of(first, second));

        PreparationGroupFinalizationDto result = preparationGroupService.finalizeGroup(7L);

        assertThat(result.getCommandesFinalisees()).isEqualTo(2);
        assertThat(result.getCommandesExcluesIncident()).isZero();
        assertThat(result.getCommandesIgnoreesNonEligibles()).isZero();
        assertThat(first.getStatut()).isEqualTo(StatutCommande.READY);
        assertThat(second.getStatut()).isEqualTo(StatutCommande.READY);
        verify(generatedEmailService).generateOrderReady(first);
        verify(generatedEmailService).generateOrderReady(second);
    }

    @Test
    void excludesIncidentAndIgnoresAlreadyReadyOrder() {
        Commande incident = commande(21L, StatutCommande.IN_PREP, true);
        Commande alreadyReady = commande(22L, StatutCommande.READY, false);
        stubGroup(8L, List.of(incident, alreadyReady));

        PreparationGroupFinalizationDto result = preparationGroupService.finalizeGroup(8L);

        assertThat(result.getCommandesFinalisees()).isZero();
        assertThat(result.getCommandesExcluesIncident()).isEqualTo(1);
        assertThat(result.getCommandesIgnoreesNonEligibles()).isEqualTo(1);
        assertThat(incident.getStatut()).isEqualTo(StatutCommande.IN_PREP);
        verify(generatedEmailService, never()).generateOrderReady(any());
    }

    @Test
    void resolvedIncidentBecomesEligibleForNextFinalization() {
        Commande commande = commande(31L, StatutCommande.IN_PREP, true);
        commande.setMotifIncidentPreparation("Produit manquant");
        stubGroup(9L, List.of(commande));

        PreparationGroupFinalizationDto blocked = preparationGroupService.finalizeGroup(9L);
        commandeService.resolvePreparationIncident(31L);
        PreparationGroupFinalizationDto finalized = preparationGroupService.finalizeGroup(9L);

        assertThat(blocked.getCommandesExcluesIncident()).isEqualTo(1);
        assertThat(finalized.getCommandesFinalisees()).isEqualTo(1);
        assertThat(commande.getStatut()).isEqualTo(StatutCommande.READY);
        assertThat(commande.isIncidentPreparation()).isFalse();
        verify(generatedEmailService, times(1)).generateOrderReady(commande);
    }

    @Test
    void repeatedFinalizationIsIdempotent() {
        Commande commande = commande(41L, StatutCommande.IN_PREP, false);
        stubGroup(10L, List.of(commande));

        PreparationGroupFinalizationDto first = preparationGroupService.finalizeGroup(10L);
        PreparationGroupFinalizationDto second = preparationGroupService.finalizeGroup(10L);

        assertThat(first.getCommandesFinalisees()).isEqualTo(1);
        assertThat(second.getCommandesFinalisees()).isZero();
        assertThat(second.getCommandesIgnoreesNonEligibles()).isEqualTo(1);
        verify(generatedEmailService, times(1)).generateOrderReady(commande);
    }

    @Test
    void smtpFailureDoesNotRollbackReadyStatus() {
        Commande commande = commande(51L, StatutCommande.IN_PREP, false);
        stubGroup(11L, List.of(commande));
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService)
                .generateOrderReady(commande);

        PreparationGroupFinalizationDto result = preparationGroupService.finalizeGroup(11L);

        assertThat(result.getCommandesFinalisees()).isEqualTo(1);
        assertThat(commande.getStatut()).isEqualTo(StatutCommande.READY);
        verify(commandeRepository).save(commande);
    }

    @Test
    void rejectsUnknownCollectionSlot() {
        when(creneauCollecteRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> preparationGroupService.finalizeGroup(404L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(commandeRepository, never()).findIdsByCreneauCollecteId(any());
    }

    @Test
    void pdfUsesOnlyOperationalPreparationStatuses() {
        CreneauCollecte creneau = CreneauCollecte.builder().id(71L).build();
        List<Commande> commandes = List.of(
                commande(61L, StatutCommande.IN_PREP, false),
                commande(62L, StatutCommande.READY, false)
        );
        PreparationPdfDocument expected = new PreparationPdfDocument("preparation.pdf", new byte[] { 1, 2, 3 });
        when(creneauCollecteRepository.findById(71L)).thenReturn(Optional.of(creneau));
        when(commandeRepository.findByCreneauCollecte_IdAndStatutInOrderByNumeroAsc(
                eq(71L),
                eq(List.of(StatutCommande.IN_PREP, StatutCommande.READY))
        )).thenReturn(commandes);
        when(preparationPdfService.generate(creneau, commandes)).thenReturn(expected);

        PreparationPdfDocument result = preparationGroupService.generatePdf(71L);

        assertThat(result).isSameAs(expected);
        verify(preparationPdfService).generate(creneau, commandes);
        verify(commandeRepository, never()).save(any());
    }

    private void stubGroup(Long creneauId, List<Commande> commandes) {
        when(creneauCollecteRepository.existsById(creneauId)).thenReturn(true);
        when(commandeRepository.findIdsByCreneauCollecteId(creneauId))
                .thenReturn(commandes.stream().map(Commande::getId).toList());
        for (Commande commande : commandes) {
            when(commandeRepository.findByIdForUpdate(commande.getId())).thenReturn(Optional.of(commande));
        }
        lenient().when(commandeRepository.save(any(Commande.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Commande commande(Long id, StatutCommande statut, boolean incident) {
        return Commande.builder()
                .id(id)
                .numero("PC01-TEST-" + id)
                .statut(statut)
                .incidentPreparation(incident)
                .dateRetraitPrevu(LocalDateTime.now().plusDays(1))
                .build();
    }
}
