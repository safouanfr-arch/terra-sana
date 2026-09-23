package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailSettingsDto {

    private String nomEntreprise;
    private String adresseEmail;
    private String telephone;
    private String adressePostale;
    private String mentionsLegales;
    private EmailTemplateDto confirmationCommande;
    private EmailTemplateDto rappelJ1;
    private EmailTemplateDto annulationCommande;
    private EmailTemplateDto validationAdhesion;
}
