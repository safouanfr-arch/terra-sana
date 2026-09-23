package com.example.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreDto {

    private Long id;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String telephone;

    @NotBlank
    private String adresse;

    @NotBlank
    private String ville;

    @NotBlank
    private String codePostal;

    private String pays;

    private String motDePasse;

    private Boolean actif;

    private LocalDateTime dateCreation;

    private LocalDateTime dateDerniereConnexion;

    private Long nbCommandes;
}
