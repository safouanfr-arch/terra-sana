package com.example.demo.mapper;

import com.example.demo.dto.DemandeAdhesionCreateDto;
import com.example.demo.dto.DemandeAdhesionDto;
import com.example.demo.model.DemandeAdhesion;

public final class DemandeAdhesionMapper {

    private DemandeAdhesionMapper() {
    }

    public static DemandeAdhesion toEntity(DemandeAdhesionCreateDto dto) {
        return DemandeAdhesion.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .codePostal(dto.getCodePostal())
                .pays(dto.getPays())
                .message(dto.getMessage())
                .build();
    }

    public static DemandeAdhesionDto toDto(DemandeAdhesion entity) {
        return DemandeAdhesionDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .email(entity.getEmail())
                .telephone(entity.getTelephone())
                .adresse(entity.getAdresse())
                .ville(entity.getVille())
                .codePostal(entity.getCodePostal())
                .pays(entity.getPays())
                .message(entity.getMessage())
                .messageRefus(entity.getMessageRefus())
                .statut(entity.getStatut())
                .dateSoumission(entity.getDateSoumission())
                .dateTraitement(entity.getDateTraitement())
                .administrateurEmail(entity.getAdministrateur() != null ? entity.getAdministrateur().getEmail() : null)
                .build();
    }
}
