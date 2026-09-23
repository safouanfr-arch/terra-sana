package com.example.demo.mapper;

import java.util.LinkedHashSet;
import java.util.List;

import com.example.demo.dto.ProduitDto;
import com.example.demo.model.Categorie;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.TagAlimentaire;

public class ProduitMapper {

    public static Produit toEntity(ProduitDto dto, Categorie categorie) {
        return Produit.builder()
                .nom(dto.getNom())
                .description(dto.getDescription())
                .prixUnitaire(dto.getPrixUnitaire())
                .unite(dto.getUnite())
                .tauxTva(dto.getTauxTva())
                .stockActuel(dto.getStockActuel())
                .images(dto.getImages())
                .actif(dto.getActif())
                .tagsAlimentaires(toTagSet(dto.getTagsAlimentaires()))
                .categorie(categorie)
                .build();
    }

    public static ProduitDto toDto(Produit entity) {
        return ProduitDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .prixUnitaire(entity.getPrixUnitaire())
                .unite(entity.getUnite())
                .tauxTva(entity.getTauxTva())
                .stockActuel(entity.getStockActuel())
                .images(entity.getImages())
                .actif(entity.getActif())
                .tagsAlimentaires(entity.getTagsAlimentaires() == null ? List.of() : entity.getTagsAlimentaires().stream().toList())
                .categorieId(entity.getCategorie().getId())
                .build();
    }

    public static void updateEntityFromDto(Produit produit, ProduitDto dto, Categorie categorie) {
        if (dto.getNom() != null) produit.setNom(dto.getNom());
        if (dto.getDescription() != null) produit.setDescription(dto.getDescription());
        if (dto.getPrixUnitaire() != null) produit.setPrixUnitaire(dto.getPrixUnitaire());
        if (dto.getUnite() != null) produit.setUnite(dto.getUnite());
        if (dto.getTauxTva() != null) produit.setTauxTva(dto.getTauxTva());
        if (dto.getStockActuel() != null) produit.setStockActuel(dto.getStockActuel());
        if (dto.getImages() != null) produit.setImages(dto.getImages());
        if (dto.getActif() != null) produit.setActif(dto.getActif());
        if (dto.getTagsAlimentaires() != null) produit.setTagsAlimentaires(toTagSet(dto.getTagsAlimentaires()));
        if (categorie != null) produit.setCategorie(categorie);
    }

    private static LinkedHashSet<TagAlimentaire> toTagSet(List<TagAlimentaire> tags) {
        return tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }
}
