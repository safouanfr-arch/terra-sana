package com.example.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dao.PromotionCategorieRepository;
import com.example.demo.dao.PromotionProduitRepository;
import com.example.demo.dao.PromotionRepository;
import com.example.demo.dto.PromotionDto;
import com.example.demo.dto.ProduitDto;
import com.example.demo.model.Categorie;
import com.example.demo.model.Produit;
import com.example.demo.model.Promotion;
import com.example.demo.model.PromotionCategorie;
import com.example.demo.model.PromotionProduit;
import com.example.demo.model.enums.TypeReduction;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final PromotionRepository promotionRepository;
    private final PromotionProduitRepository promotionProduitRepository;
    private final PromotionCategorieRepository promotionCategorieRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;

    @Transactional(readOnly = true)
    public List<PromotionDto> getAll() {
        return promotionRepository.findAllByOrderByDateDebutDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromotionCalculation calculateForProduit(Produit produit) {
        BigDecimal prixOriginal = scale(produit.getPrixUnitaire());
        Promotion bestPromotion = null;
        BigDecimal bestPrix = prixOriginal;
        BigDecimal bestReduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (Promotion promotion : promotionRepository.findActiveAt(LocalDateTime.now())) {
            if (!appliesToProduit(promotion, produit)) {
                continue;
            }

            BigDecimal prixApresReduction = calculateDiscountedPrice(prixOriginal, promotion, produit);
            BigDecimal reduction = scale(prixOriginal.subtract(prixApresReduction));
            if (reduction.compareTo(bestReduction) > 0) {
                bestPromotion = promotion;
                bestPrix = prixApresReduction;
                bestReduction = reduction;
            }
        }

        if (bestPromotion == null) {
            return new PromotionCalculation(
                    null,
                    null,
                    null,
                    null,
                    prixOriginal,
                    prixOriginal,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        return new PromotionCalculation(
                bestPromotion.getId(),
                bestPromotion.getNom(),
                bestPromotion.getTypeReduction().name(),
                scale(bestPromotion.getValeurReduction()),
                prixOriginal,
                bestPrix,
                bestReduction
        );
    }

    @Transactional(readOnly = true)
    public ProduitDto applyPromotionToDto(Produit produit, ProduitDto dto) {
        PromotionCalculation calculation = calculateForProduit(produit);
        dto.setPrixAvantPromotion(calculation.prixOriginal());
        dto.setPrixPromotionnel(calculation.prixFinal());
        dto.setReductionMontant(calculation.reductionMontant());
        dto.setPromotionId(calculation.promotionId());
        dto.setPromotionNom(calculation.promotionNom());
        dto.setPromotionTypeReduction(calculation.typeReduction());
        dto.setPromotionValeurReduction(calculation.valeurReduction());
        return dto;
    }

    @Transactional
    public PromotionDto create(PromotionDto dto) {
        Promotion promotion = Promotion.builder().build();
        applyDto(promotion, dto);
        Promotion saved = promotionRepository.save(promotion);
        syncLinks(saved, dto);
        return toDto(promotionRepository.findById(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable apres creation.")));
    }

    @Transactional
    public PromotionDto update(Long id, PromotionDto dto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable."));

        applyDto(promotion, dto);
        Promotion saved = promotionRepository.save(promotion);
        syncLinks(saved, dto);
        return toDto(promotionRepository.findById(saved.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable apres mise a jour.")));
    }

    @Transactional
    public void delete(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion introuvable.");
        }
        promotionProduitRepository.deleteByPromotion_Id(id);
        promotionCategorieRepository.deleteByPromotion_Id(id);
        promotionRepository.deleteById(id);
    }

    private void applyDto(Promotion promotion, PromotionDto dto) {
        if (dto.getDateDebut() == null || dto.getDateFin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les dates de promotion sont requises.");
        }
        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de fin doit etre posterieure ou egale a la date de debut.");
        }
        if (dto.getValeurReduction() == null || dto.getValeurReduction().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reduction doit etre strictement positive.");
        }

        TypeReduction typeReduction = parseTypeReduction(dto.getTypeReduction());
        if (typeReduction == TypeReduction.POURCENTAGE && dto.getValeurReduction().compareTo(ONE_HUNDRED) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une reduction en pourcentage ne peut pas depasser 100.");
        }

        promotion.setNom(trimRequired(dto.getNom(), "Le nom de la promotion est requis."));
        promotion.setDescription(trimToNull(dto.getDescription()));
        promotion.setTypeReduction(typeReduction);
        promotion.setValeurReduction(dto.getValeurReduction());
        promotion.setDateDebut(LocalDateTime.of(dto.getDateDebut(), LocalTime.MIN));
        promotion.setDateFin(LocalDateTime.of(dto.getDateFin(), LocalTime.of(23, 59, 59)));
        promotion.setActif(dto.getActif() == null || dto.getActif());
    }

    private void syncLinks(Promotion promotion, PromotionDto dto) {
        promotionProduitRepository.deleteByPromotion_Id(promotion.getId());
        promotionCategorieRepository.deleteByPromotion_Id(promotion.getId());
        promotionProduitRepository.flush();
        promotionCategorieRepository.flush();

        Set<Long> produitIds = distinctIds(dto.getProduitIds());
        Set<Long> categorieIds = distinctIds(dto.getCategorieIds());

        if (!produitIds.isEmpty()) {
            List<Produit> produits = produitRepository.findAllById(produitIds);
            if (produits.size() != produitIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un ou plusieurs produits selectionnes sont introuvables.");
            }

            List<PromotionProduit> links = produits.stream()
                    .map(produit -> PromotionProduit.builder()
                            .promotion(promotion)
                            .produit(produit)
                            .build())
                    .toList();
            promotionProduitRepository.saveAll(links);
        }

        if (!categorieIds.isEmpty()) {
            List<Categorie> categories = categorieRepository.findAllById(categorieIds);
            if (categories.size() != categorieIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une ou plusieurs categories selectionnees sont introuvables.");
            }

            List<PromotionCategorie> links = categories.stream()
                    .map(categorie -> PromotionCategorie.builder()
                            .promotion(promotion)
                            .categorie(categorie)
                            .build())
                    .toList();
            promotionCategorieRepository.saveAll(links);
        }
    }

    private PromotionDto toDto(Promotion promotion) {
        List<Long> produitIds = new ArrayList<>();
        for (PromotionProduit link : promotion.getProduits()) {
            if (link.getProduit() != null && link.getProduit().getId() != null) {
                produitIds.add(link.getProduit().getId());
            }
        }

        List<Long> categorieIds = new ArrayList<>();
        for (PromotionCategorie link : promotion.getCategories()) {
            if (link.getCategorie() != null && link.getCategorie().getId() != null) {
                categorieIds.add(link.getCategorie().getId());
            }
        }

        return PromotionDto.builder()
                .id(promotion.getId())
                .nom(promotion.getNom())
                .description(promotion.getDescription())
                .typeReduction(promotion.getTypeReduction().name())
                .valeurReduction(promotion.getValeurReduction())
                .dateDebut(promotion.getDateDebut().toLocalDate())
                .dateFin(promotion.getDateFin().toLocalDate())
                .actif(Boolean.TRUE.equals(promotion.getActif()))
                .produitIds(produitIds)
                .categorieIds(categorieIds)
                .build();
    }

    private boolean appliesToProduit(Promotion promotion, Produit produit) {
        Long produitId = produit.getId();
        Long categorieId = produit.getCategorie() != null ? produit.getCategorie().getId() : null;

        boolean appliesToProduit = promotion.getProduits() != null && promotion.getProduits().stream()
                .anyMatch(link -> link.getProduit() != null && produitId.equals(link.getProduit().getId()));
        if (appliesToProduit) {
            return true;
        }

        return promotion.getCategories() != null && promotion.getCategories().stream()
                .anyMatch(link -> link.getCategorie() != null && categorieId != null && categorieId.equals(link.getCategorie().getId()));
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal prixOriginal, Promotion promotion, Produit produit) {
        BigDecimal prixFinal = switch (promotion.getTypeReduction()) {
            case POURCENTAGE -> prixOriginal.subtract(prixOriginal
                    .multiply(promotion.getValeurReduction())
                    .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP));
            case MONTANT_FIXE -> prixOriginal.subtract(toHtDiscount(promotion.getValeurReduction(), produit));
        };

        if (prixFinal.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scale(prixFinal);
    }

    private BigDecimal toHtDiscount(BigDecimal montantTtc, Produit produit) {
        BigDecimal tauxTva = produit.getTauxTva() == null ? BigDecimal.ZERO : produit.getTauxTva();
        BigDecimal divisor = BigDecimal.ONE.add(tauxTva.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return montantTtc;
        }
        return montantTtc.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private TypeReduction parseTypeReduction(String value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type de reduction est requis.");
        }

        return switch (value.trim().toUpperCase()) {
            case "POURCENTAGE" -> TypeReduction.POURCENTAGE;
            case "MONTANT_FIXE", "MONTANT" -> TypeReduction.MONTANT_FIXE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de reduction invalide.");
        };
    }

    private Set<Long> distinctIds(List<Long> ids) {
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }

    private String trimRequired(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
