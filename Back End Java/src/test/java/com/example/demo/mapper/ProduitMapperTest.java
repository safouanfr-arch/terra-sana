package com.example.demo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.demo.dto.ProduitDto;
import com.example.demo.model.Categorie;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.TagAlimentaire;
import com.example.demo.model.enums.UniteProduit;

public class ProduitMapperTest {

    @Test
    void toEntityMapsTagsAlimentaires() {
        ProduitDto dto = ProduitDto.builder()
                .nom("Carottes bio")
                .description("Carottes locales")
                .prixUnitaire(new BigDecimal("3.60"))
                .unite(UniteProduit.KG)
                .tauxTva(new BigDecimal("6.00"))
                .stockActuel(new BigDecimal("12.00"))
                .images("{\"image\":\"C\"}")
                .actif(true)
                .tagsAlimentaires(List.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL))
                .categorieId(1L)
                .build();

        Categorie categorie = Categorie.builder()
                .id(1L)
                .nom("Legumes")
                .build();

        Produit produit = ProduitMapper.toEntity(dto, categorie);

        assertThat(produit.getTagsAlimentaires())
                .containsExactlyInAnyOrder(TagAlimentaire.BIO, TagAlimentaire.LOCAL);
        assertThat(produit.getCategorie()).isSameAs(categorie);
    }

    @Test
    void toDtoMapsTagsAlimentaires() {
        Categorie categorie = Categorie.builder()
                .id(4L)
                .nom("Epicerie")
                .build();

        Produit produit = Produit.builder()
                .Id(8L)
                .nom("Granola")
                .description("Petit dejeuner")
                .prixUnitaire(new BigDecimal("5.40"))
                .unite(UniteProduit.PIECE)
                .tauxTva(new BigDecimal("6.00"))
                .stockActuel(new BigDecimal("7.00"))
                .images("{\"image\":\"G\"}")
                .actif(true)
                .tagsAlimentaires(EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.VEGAN))
                .categorie(categorie)
                .build();

        ProduitDto dto = ProduitMapper.toDto(produit);

        assertThat(dto.getId()).isEqualTo(8L);
        assertThat(dto.getCategorieId()).isEqualTo(4L);
        assertThat(dto.getTagsAlimentaires())
                .containsExactlyInAnyOrder(TagAlimentaire.BIO, TagAlimentaire.VEGAN);
    }
}
