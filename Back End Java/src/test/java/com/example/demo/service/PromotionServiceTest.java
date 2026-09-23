package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dao.PromotionCategorieRepository;
import com.example.demo.dao.PromotionProduitRepository;
import com.example.demo.dao.PromotionRepository;
import com.example.demo.model.Categorie;
import com.example.demo.model.Produit;
import com.example.demo.model.Promotion;
import com.example.demo.model.PromotionCategorie;
import com.example.demo.model.PromotionProduit;
import com.example.demo.model.enums.TypeReduction;
import com.example.demo.model.enums.UniteProduit;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionProduitRepository promotionProduitRepository;

    @Mock
    private PromotionCategorieRepository promotionCategorieRepository;

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private CategorieRepository categorieRepository;

    @InjectMocks
    private PromotionService promotionService;

    @Test
    void calculateForProduitKeepsBestActivePromotion() {
        Categorie categorie = Categorie.builder().id(2L).nom("Legumes").build();
        Produit produit = Produit.builder()
                .Id(8L)
                .nom("Carottes")
                .prixUnitaire(new BigDecimal("10.00"))
                .tauxTva(new BigDecimal("6.00"))
                .unite(UniteProduit.KG)
                .categorie(categorie)
                .build();

        Promotion categoryPromotion = Promotion.builder()
                .id(1L)
                .nom("Categorie -10%")
                .typeReduction(TypeReduction.POURCENTAGE)
                .valeurReduction(new BigDecimal("10.00"))
                .dateDebut(LocalDateTime.now().minusDays(1))
                .dateFin(LocalDateTime.now().plusDays(1))
                .actif(true)
                .build();
        categoryPromotion.setCategories(List.of(PromotionCategorie.builder()
                .promotion(categoryPromotion)
                .categorie(categorie)
                .build()));

        Promotion productPromotion = Promotion.builder()
                .id(2L)
                .nom("Produit -3 EUR")
                .typeReduction(TypeReduction.MONTANT_FIXE)
                .valeurReduction(new BigDecimal("3.00"))
                .dateDebut(LocalDateTime.now().minusDays(1))
                .dateFin(LocalDateTime.now().plusDays(1))
                .actif(true)
                .build();
        productPromotion.setProduits(List.of(PromotionProduit.builder()
                .promotion(productPromotion)
                .produit(produit)
                .build()));

        when(promotionRepository.findActiveAt(org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(categoryPromotion, productPromotion));

        PromotionCalculation result = promotionService.calculateForProduit(produit);

        assertThat(result.hasPromotion()).isTrue();
        assertThat(result.promotionId()).isEqualTo(2L);
        assertThat(result.prixOriginal()).isEqualByComparingTo("10.00");
        assertThat(result.prixFinal()).isEqualByComparingTo("7.17");
        assertThat(result.reductionMontant()).isEqualByComparingTo("2.83");
        assertThat(result.valeurReduction()).isEqualByComparingTo("3.00");
    }
}
