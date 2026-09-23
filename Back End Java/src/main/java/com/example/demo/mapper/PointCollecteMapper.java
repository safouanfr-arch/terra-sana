package com.example.demo.mapper;

import com.example.demo.dto.PointCollecteDto;
import com.example.demo.dto.CreneauCollecteDto;
import com.example.demo.model.PointCollecte;

import java.util.List;

public class PointCollecteMapper {

    public static PointCollecteDto toDto(PointCollecte entity) {

        List<CreneauCollecteDto> creneauxDto = entity.getCreneaux()
                .stream()
                .map(CreneauCollecteMapper::toDto)
                .toList();

        return PointCollecteDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .adresse(entity.getAdresse())
                .actif(entity.getActif())
                .creneaux(creneauxDto)   // juste en lecture
                .build();
    }

    public static PointCollecte toEntity(PointCollecteDto dto) {
        return PointCollecte.builder()
                .nom(dto.getNom())
                .adresse(dto.getAdresse())
                .actif(dto.getActif())
                // on ne mappe PAS les créneaux ici
                .build();
    }

    public static void updateEntityFromDto(PointCollecte entity, PointCollecteDto dto) {
        if (dto.getNom() != null) entity.setNom(dto.getNom());
        if (dto.getAdresse() != null) entity.setAdresse(dto.getAdresse());
        if (dto.getActif() != null) entity.setActif(dto.getActif());
        // toujours pas de gestion des créneaux ici
    }
}
