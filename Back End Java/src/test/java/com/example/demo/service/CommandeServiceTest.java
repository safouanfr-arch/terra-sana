package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.LigneCommandeRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.CommandeCreateItemDto;
import com.example.demo.dto.CommandeCreateRequestDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.model.Commande;
import com.example.demo.model.Categorie;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.MouvementStock;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.UniteProduit;

@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

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

    @InjectMocks
    private CommandeService commandeService;

    private static final ZoneId TEST_ZONE = ZoneId.systemDefault();
    private LocalDateTime fixedNow;

    @BeforeEach
    void setUpClock() {
        fixedNow = LocalDate.now().atTime(8, 0);
        Clock fixedClock = Clock.fixed(fixedNow.atZone(TEST_ZONE).toInstant(), TEST_ZONE);
        ReflectionTestUtils.setField(commandeService, "clock", fixedClock);
    }

    @Test
    void createAllowsOrderWhenPointCollecteIsActiveAndRetraitIsExactly24HoursAway() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusHours(24), true);
        Produit produit = produitAvecStock("10.00");

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(produitRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(produit));
        when(promotionService.calculateForProduit(produit)).thenReturn(promotionSansReduction(produit));
        stubSuccessfulPersistence();

        CommandeResponseDto response = commandeService.create(payloadQuantite("2.00"));

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getNumero()).startsWith("PC01-");
        assertThat(produit.getStockActuel()).isEqualByComparingTo("8.00");
        verify(produitRepository).findByIdForUpdate(8L);
    }

    @Test
    void createAllowsOrderWhenRetraitIsInsideAllowedWindow() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusDays(3), true);
        Produit produit = produitAvecStock("10.00");

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(produitRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(produit));
        when(promotionService.calculateForProduit(produit)).thenReturn(promotionSansReduction(produit));
        stubSuccessfulPersistence();

        CommandeResponseDto response = commandeService.create(payloadQuantite("2.00"));

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(produit.getStockActuel()).isEqualByComparingTo("8.00");
    }

    @Test
    void createAllowsOrderWhenRetraitIsExactly8DaysAway() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusDays(8), true);
        Produit produit = produitAvecStock("10.00");

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(produitRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(produit));
        when(promotionService.calculateForProduit(produit)).thenReturn(promotionSansReduction(produit));
        stubSuccessfulPersistence();

        CommandeResponseDto response = commandeService.create(payloadQuantite("2.00"));

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(produit.getStockActuel()).isEqualByComparingTo("8.00");
    }

    @Test
    void createRejectsOrderWhenPointCollecteIsInactiveWithoutChangingStock() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusDays(2), false);

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));

        assertThatThrownBy(() -> commandeService.create(payloadQuantite("2.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("point de collecte");
                });

        verify(produitRepository, never()).findByIdForUpdate(any());
        verify(produitRepository, never()).save(any());
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void createRejectsOrderWhenRetraitIsLessThan24HoursAwayWithoutChangingStock() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusHours(23).plusMinutes(59), true);

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));

        assertThatThrownBy(() -> commandeService.create(payloadQuantite("1.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("24 heures");
                });

        verify(produitRepository, never()).findByIdForUpdate(any());
        verify(produitRepository, never()).save(any());
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void createRejectsOrderWhenRetraitIsAlreadyPastWithoutChangingStock() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.minusHours(1), true);

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));

        assertThatThrownBy(() -> commandeService.create(payloadQuantite("1.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("deja passe");
                });

        verify(produitRepository, never()).findByIdForUpdate(any());
        verify(produitRepository, never()).save(any());
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void createRejectsOrderWhenRetraitIsMoreThan8DaysAwayWithoutChangingStock() {
        Membre membre = membreActif();
        CreneauCollecte creneau = creneauAt(fixedNow.plusDays(8).plusMinutes(1), true);

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));

        assertThatThrownBy(() -> commandeService.create(payloadQuantite("1.00")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("8 jours");
                });

        verify(produitRepository, never()).findByIdForUpdate(any());
        verify(produitRepository, never()).save(any());
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void createReturnsConflictWhenStockChangedBeforeValidation() {
        Membre membre = Membre.builder()
                .id(4L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .actif(true)
                .build();
        CreneauCollecte creneau = CreneauCollecte.builder()
                .id(7L)
                .creneau(LocalDate.now().plusDays(1))
                .heureDebut(LocalTime.of(18, 0))
                .heureFin(LocalTime.of(20, 0))
                .actif(true)
                .pointCollecte(PointCollecte.builder().id(1L).nom("Depot").adresse("Rue Test").actif(true).build())
                .build();
        Produit produit = Produit.builder()
                .Id(8L)
                .nom("Carottes bio")
                .prixUnitaire(new BigDecimal("3.60"))
                .unite(UniteProduit.KG)
                .stockActuel(new BigDecimal("1.00"))
                .categorie(Categorie.builder().id(2L).nom("Legumes").actif(true).build())
                .actif(true)
                .build();

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(produitRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(produit));

        CommandeCreateRequestDto payload = CommandeCreateRequestDto.builder()
                .membreId(4L)
                .creneauCollecteId(7L)
                .produits(List.of(
                        CommandeCreateItemDto.builder()
                                .produitId(8L)
                                .quantite(new BigDecimal("2.00"))
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> commandeService.create(payload))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).contains("Stock insuffisant");
                });

        verify(produitRepository).findByIdForUpdate(8L);
        verify(commandeRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createKeepsCommandeWhenEmailGenerationFails() {
        Membre membre = Membre.builder()
                .id(4L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .actif(true)
                .build();
        CreneauCollecte creneau = CreneauCollecte.builder()
                .id(7L)
                .creneau(LocalDate.now().plusDays(1))
                .heureDebut(LocalTime.of(18, 0))
                .heureFin(LocalTime.of(20, 0))
                .actif(true)
                .pointCollecte(PointCollecte.builder().id(1L).nom("Depot").adresse("Rue Test").actif(true).build())
                .build();
        Produit produit = Produit.builder()
                .Id(8L)
                .nom("Carottes bio")
                .prixUnitaire(new BigDecimal("3.60"))
                .tauxTva(new BigDecimal("6.00"))
                .unite(UniteProduit.KG)
                .stockActuel(new BigDecimal("10.00"))
                .categorie(Categorie.builder().id(2L).nom("Legumes").actif(true).build())
                .actif(true)
                .build();

        when(membreRepository.findById(4L)).thenReturn(Optional.of(membre));
        when(creneauCollecteRepository.findById(7L)).thenReturn(Optional.of(creneau));
        when(produitRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(produit));
        when(promotionService.calculateForProduit(produit)).thenReturn(new PromotionCalculation(
                null,
                "",
                "",
                BigDecimal.ZERO,
                new BigDecimal("3.60"),
                new BigDecimal("3.60"),
                BigDecimal.ZERO
        ));
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> {
            Commande commande = invocation.getArgument(0);
            if (commande.getId() == null) {
                commande.setId(99L);
            }
            return commande;
        });
        when(ligneCommandeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService)
                .generateOrderConfirmation(any(Commande.class));

        CommandeCreateRequestDto payload = CommandeCreateRequestDto.builder()
                .membreId(4L)
                .creneauCollecteId(7L)
                .produits(List.of(
                        CommandeCreateItemDto.builder()
                                .produitId(8L)
                                .quantite(new BigDecimal("2.00"))
                                .build()
                ))
                .build();

        CommandeResponseDto response = commandeService.create(payload);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getNumero()).startsWith("PC01-");
        assertThat(produit.getStockActuel()).isEqualByComparingTo("8.00");
        verify(generatedEmailService).generateOrderConfirmation(any(Commande.class));
        verify(ligneCommandeRepository).saveAll(anyList());
    }

    @Test
    void cancelForMembreRejectsOrderOwnedByAnotherMember() {
        Commande commande = Commande.builder()
                .id(99L)
                .membre(Membre.builder().id(12L).build())
                .build();
        when(commandeRepository.findById(99L)).thenReturn(Optional.of(commande));

        assertThatThrownBy(() -> commandeService.cancelForMembre(99L, 4L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("n'appartient pas");

        verify(commandeRepository, never()).save(any(Commande.class));
        verify(produitRepository, never()).findByIdForUpdate(any());
    }

    private Membre membreActif() {
        return Membre.builder()
                .id(4L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .actif(true)
                .build();
    }

    private CreneauCollecte creneauAt(LocalDateTime retrait, boolean pointActif) {
        return CreneauCollecte.builder()
                .id(7L)
                .creneau(retrait.toLocalDate())
                .heureDebut(retrait.toLocalTime())
                .heureFin(retrait.toLocalTime().plusHours(2))
                .actif(true)
                .pointCollecte(PointCollecte.builder()
                        .id(1L)
                        .nom("Depot")
                        .adresse("Rue Test")
                        .actif(pointActif)
                        .build())
                .build();
    }

    private Produit produitAvecStock(String stock) {
        return Produit.builder()
                .Id(8L)
                .nom("Carottes bio")
                .prixUnitaire(new BigDecimal("3.60"))
                .tauxTva(new BigDecimal("6.00"))
                .unite(UniteProduit.KG)
                .stockActuel(new BigDecimal(stock))
                .categorie(Categorie.builder().id(2L).nom("Legumes").actif(true).build())
                .actif(true)
                .build();
    }

    private PromotionCalculation promotionSansReduction(Produit produit) {
        return new PromotionCalculation(
                null,
                "",
                "",
                BigDecimal.ZERO,
                produit.getPrixUnitaire(),
                produit.getPrixUnitaire(),
                BigDecimal.ZERO
        );
    }

    private CommandeCreateRequestDto payloadQuantite(String quantite) {
        return CommandeCreateRequestDto.builder()
                .membreId(4L)
                .creneauCollecteId(7L)
                .produits(List.of(
                        CommandeCreateItemDto.builder()
                                .produitId(8L)
                                .quantite(new BigDecimal(quantite))
                                .build()
                ))
                .build();
    }

    private void stubSuccessfulPersistence() {
        when(commandeRepository.save(any(Commande.class))).thenAnswer(invocation -> {
            Commande commande = invocation.getArgument(0);
            if (commande.getId() == null) {
                commande.setId(99L);
            }
            return commande;
        });
        when(ligneCommandeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mouvementStockRepository.save(any(MouvementStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
