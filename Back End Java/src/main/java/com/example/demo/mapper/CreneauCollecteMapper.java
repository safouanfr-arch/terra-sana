package com.example.demo.mapper;

import com.example.demo.dto.CreneauCollecteDto;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;

public class CreneauCollecteMapper {

    public static CreneauCollecteDto toDto(CreneauCollecte entity) {
        return CreneauCollecteDto.builder()
                .id(entity.getId())
                .pointCollecteId(entity.getPointCollecte().getId())
                .date(entity.getCreneau())
                .heureDebut(entity.getHeureDebut())
                .heureFin(entity.getHeureFin())
                .actif(entity.getActif())
                .build();
    }

  
    public static CreneauCollecte toEntity(CreneauCollecteDto dto, PointCollecte pointCollecte) {
        return CreneauCollecte.builder()
        		.pointCollecte(pointCollecte)
                .creneau(dto.getDate())
                .heureDebut(dto.getHeureDebut())
                .heureFin(dto.getHeureFin())
                .actif(dto.getActif())
                .build();
    }
    
    public static void updateEntityFromDto(CreneauCollecte creneauCollecte, CreneauCollecteDto dto, PointCollecte pointCollecte) {
    	if (pointCollecte != null) creneauCollecte.setPointCollecte(pointCollecte);
        if (dto.getDate() != null) creneauCollecte.setCreneau(dto.getDate());
	    if (dto.getHeureDebut() != null) creneauCollecte.setHeureDebut(dto.getHeureDebut());
	    if (dto.getHeureFin() != null) creneauCollecte.setHeureFin(dto.getHeureFin());
	    if (dto.getActif() != null) creneauCollecte.setActif(dto.getActif());
	}
}
