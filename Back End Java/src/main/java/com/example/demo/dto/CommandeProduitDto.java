package com.example.demo.dto;

import java.math.BigDecimal;

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
public class CommandeProduitDto {

    private Long id;
    private Long produitId;
    private String nom;
    private String unite;
    private BigDecimal quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal prixUnitaireOriginal;
    private BigDecimal reductionUnitaire;
    private Long promotionId;
    private String promotionNom;
    private BigDecimal tauxTVA;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
}
