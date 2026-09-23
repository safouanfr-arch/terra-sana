package com.example.demo.mapper;

import com.example.demo.dto.CategorieDto;
import com.example.demo.model.Categorie;

public class CategorieMapper {

    public static CategorieDto toDto(Categorie entity) {
        if (entity == null) return null;
        return CategorieDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .actif(entity.getActif()
                )
                .build();
    }

    public static Categorie fromDto(CategorieDto dto) {
        if (dto == null) return null;
        Categorie c = new Categorie();
        c.setNom(dto.getNom());
        c.setDescription(dto.getDescription());
        // si null → true par défaut
        c.setActif(dto.getActif() != null ? dto.getActif() : true);
        return c;
    }

    public static void updateEntityFromDto(Categorie entity, CategorieDto dto) {
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        if (dto.getActif() != null) {
            entity.setActif(dto.getActif());
        }
    }
}
