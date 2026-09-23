package com.example.demo.mapper;

import com.example.demo.dto.MembreDto;
import com.example.demo.model.Membre;

public class MembreMapper {
	 public static MembreDto toDto(Membre entity) {
	        return MembreDto.builder()
	                .id(entity.getId())
	                .nom(entity.getNom())
	                .prenom(entity.getPrenom())
	                .email(entity.getEmail())
	                .telephone(entity.getTelephone())
	                .adresse(entity.getAdresse())
	                .ville(entity.getVille())
	                .codePostal(entity.getCodePostal())
	                .pays(entity.getPays())
	                .motDePasse(null)
	                .actif(entity.getActif())
	                .dateCreation(entity.getDateCreation())
	                .dateDerniereConnexion(entity.getDateLogin())
	                .build();
	    }

	 public static Membre toEntity(MembreDto dto) {
			return Membre.builder()
					.nom(dto.getNom())
	                .prenom(dto.getPrenom())
	                .email(dto.getEmail())
	                .telephone(dto.getTelephone())
	                .adresse(dto.getAdresse())
	                .ville(dto.getVille())
	                .codePostal(dto.getCodePostal())
	                .pays(dto.getPays())
	                .motDePasse(dto.getMotDePasse())
	                .actif(dto.getActif())
					.build() ;
		}

	 public static void updateEntityFromDto(Membre membre, MembreDto dto) {

		    if (dto.getNom() != null) membre.setNom(dto.getNom());
		    if (dto.getPrenom() != null) membre.setPrenom(dto.getPrenom());
		    if (dto.getEmail() != null) membre.setEmail(dto.getEmail());
		    if (dto.getTelephone() != null) membre.setTelephone(dto.getTelephone());
		    if (dto.getAdresse() != null) membre.setAdresse(dto.getAdresse());
		    if (dto.getVille() != null) membre.setVille(dto.getVille());
		    if (dto.getCodePostal() != null) membre.setCodePostal(dto.getCodePostal());
		    if (dto.getPays() != null) membre.setPays(dto.getPays());
		    if (dto.getActif() != null) membre.setActif(dto.getActif());
		}
	
}
