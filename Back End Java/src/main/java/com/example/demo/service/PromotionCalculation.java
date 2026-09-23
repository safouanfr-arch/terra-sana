package com.example.demo.service;

import java.math.BigDecimal;

public record PromotionCalculation(
        Long promotionId,
        String promotionNom,
        String typeReduction,
        BigDecimal valeurReduction,
        BigDecimal prixOriginal,
        BigDecimal prixFinal,
        BigDecimal reductionMontant
) {

    public boolean hasPromotion() {
        return promotionId != null && reductionMontant != null && reductionMontant.signum() > 0;
    }
}
