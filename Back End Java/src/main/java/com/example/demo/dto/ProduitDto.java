package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.model.enums.TagAlimentaire;
import com.example.demo.model.enums.UniteProduit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitDto {
    private Long id;

    @NotBlank
    private String nom;

    private String description;

    @NotNull
    private BigDecimal prixUnitaire;

    private BigDecimal prixAvantPromotion;

    private BigDecimal prixPromotionnel;

    private BigDecimal reductionMontant;

    private Long promotionId;

    private String promotionNom;

    private String promotionTypeReduction;

    private BigDecimal promotionValeurReduction;

    @NotNull
    private UniteProduit unite;

    private BigDecimal tauxTva;

    private BigDecimal stockActuel;

    private String images;

    private Boolean actif;

    private List<TagAlimentaire> tagsAlimentaires;

    @NotNull
    private Long categorieId;
}
