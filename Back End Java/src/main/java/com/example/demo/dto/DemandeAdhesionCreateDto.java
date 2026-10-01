package com.example.demo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class DemandeAdhesionCreateDto {

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

    private String message;

    @NotNull(message = "L'acceptation des conditions est obligatoire.")
    @AssertTrue(message = "Les conditions doivent etre acceptees.")
    private Boolean accepteConditions;
}
