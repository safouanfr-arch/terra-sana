package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.enums.StatutDemande;

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
public class DemandeAdhesionDto {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String message;
    private String messageRefus;
    private StatutDemande statut;
    private LocalDateTime dateSoumission;
    private LocalDateTime dateTraitement;
    private String administrateurEmail;
    private String lienActivation;
}
