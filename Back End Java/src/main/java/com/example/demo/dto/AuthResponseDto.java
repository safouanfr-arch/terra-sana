package com.example.demo.dto;

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
public class AuthResponseDto {

    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private Boolean actif;
    private String telephone;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
}
