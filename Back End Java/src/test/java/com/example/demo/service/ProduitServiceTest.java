package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.ProduitDto;
import com.example.demo.dto.ProduitStockAdjustmentDto;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Categorie;
import com.example.demo.model.MouvementStock;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.TypeMouvementStock;
import com.example.demo.model.enums.UniteProduit;

@ExtendWith(MockitoExtension.class)
public class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private CategorieRepository categorieRepository;

    @Mock
    private MouvementStockRepository mouvementStockRepository;

    @Mock
    private AdministrateurRepository administrateurRepository;

    @Mock
    private GeneratedEmailService generatedEmailService;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private ProduitService produitService;

    @Test
    void updateNameKeepsStockUnchangedEvenWhenDtoContainsAnotherStock() {
        Produit produit = productWithStock("10.00");
        ProduitDto dto = updateDto("Carottes nouvelles", "3.60", "999.00");
        stubProductUpdate(produit);

        ProduitDto result = produitService.update(5L, dto);

        assertThat(produit.getNom()).isEqualTo("Carottes nouvelles");
        assertThat(produit.getStockActuel()).isEqualByComparingTo("10.00");
        assertThat(result.getStockActuel()).isEqualByComparingTo("10.00");
    }

    @Test
    void updatePriceKeepsStockUnchangedEvenWhenDtoContainsAnotherStock() {
        Produit produit = productWithStock("10.00");
        ProduitDto dto = updateDto("Carottes", "4.25", "0.00");
        stubProductUpdate(produit);

        ProduitDto result = produitService.update(5L, dto);

        assertThat(produit.getPrixUnitaire()).isEqualByComparingTo("4.25");
        assertThat(produit.getStockActuel()).isEqualByComparingTo("10.00");
        assertThat(result.getStockActuel()).isEqualByComparingTo("10.00");
    }

    @Test
    void adjustStockUpdatesProductAndCreatesMouvement() {
        Produit produit = Produit.builder()
                .Id(5L)
                .nom("Carottes")
                .prixUnitaire(new BigDecimal("3.60"))
                .unite(UniteProduit.KG)
                .stockActuel(new BigDecimal("10.00"))
                .categorie(Categorie.builder().id(1L).nom("Legumes").build())
                .actif(true)
                .build();
        Administrateur administrateur = Administrateur.builder()
                .Id(9L)
                .nom("Admin")
                .prenom("Demo")
                .email("admin@terra.test")
                .motDePasse("secret")
                .build();

        when(produitRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(produit));
        when(administrateurRepository.findById(9L)).thenReturn(Optional.of(administrateur));
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionService.applyPromotionToDto(eq(produit), any(ProduitDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ProduitDto result = produitService.adjustStock(
                5L,
                ProduitStockAdjustmentDto.builder().stockActuel(new BigDecimal("12.50")).build(),
                9L
        );

        assertThat(result.getStockActuel()).isEqualByComparingTo("12.50");
        assertThat(produit.getStockActuel()).isEqualByComparingTo("12.50");

        ArgumentCaptor<MouvementStock> mouvementCaptor = ArgumentCaptor.forClass(MouvementStock.class);
        verify(mouvementStockRepository).save(mouvementCaptor.capture());

        MouvementStock mouvement = mouvementCaptor.getValue();
        assertThat(mouvement.getType()).isEqualTo(TypeMouvementStock.AJUSTEMENT);
        assertThat(mouvement.getQuantite()).isEqualByComparingTo("2.50");
        assertThat(mouvement.getStockAvant()).isEqualByComparingTo("10.00");
        assertThat(mouvement.getStockApres()).isEqualByComparingTo("12.50");
        assertThat(mouvement.getProduit()).isSameAs(produit);
        assertThat(mouvement.getAdministrateur()).isSameAs(administrateur);
    }

    @Test
    void adjustStockRejectsNegativeValue() {
        Produit produit = Produit.builder()
                .Id(5L)
                .nom("Carottes")
                .prixUnitaire(new BigDecimal("3.60"))
                .unite(UniteProduit.KG)
                .stockActuel(new BigDecimal("10.00"))
                .categorie(Categorie.builder().id(1L).nom("Legumes").build())
                .actif(true)
                .build();
        Administrateur administrateur = Administrateur.builder()
                .Id(9L)
                .nom("Admin")
                .prenom("Demo")
                .email("admin@terra.test")
                .motDePasse("secret")
                .build();

        when(produitRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(produit));
        when(administrateurRepository.findById(9L)).thenReturn(Optional.of(administrateur));

        assertThatThrownBy(() -> produitService.adjustStock(
                5L,
                ProduitStockAdjustmentDto.builder().stockActuel(new BigDecimal("-1.00")).build(),
                9L
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private void stubProductUpdate(Produit produit) {
        when(produitRepository.findById(5L)).thenReturn(Optional.of(produit));
        when(categorieRepository.findById(1L)).thenReturn(Optional.of(produit.getCategorie()));
        when(produitRepository.save(any(Produit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionService.applyPromotionToDto(eq(produit), any(ProduitDto.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private Produit productWithStock(String stock) {
        return Produit.builder()
                .Id(5L)
                .nom("Carottes")
                .description("Carottes locales")
                .prixUnitaire(new BigDecimal("3.60"))
                .unite(UniteProduit.KG)
                .tauxTva(new BigDecimal("6.00"))
                .stockActuel(new BigDecimal(stock))
                .categorie(Categorie.builder().id(1L).nom("Legumes").build())
                .actif(true)
                .build();
    }

    private ProduitDto updateDto(String name, String price, String stock) {
        return ProduitDto.builder()
                .nom(name)
                .description("Carottes locales")
                .prixUnitaire(new BigDecimal(price))
                .unite(UniteProduit.KG)
                .tauxTva(new BigDecimal("6.00"))
                .stockActuel(new BigDecimal(stock))
                .actif(true)
                .categorieId(1L)
                .build();
    }
}
