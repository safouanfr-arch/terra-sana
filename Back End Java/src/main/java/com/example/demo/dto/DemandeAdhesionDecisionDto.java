package com.example.demo.dto;

import com.example.demo.model.enums.StatutDemande;

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
public class DemandeAdhesionDecisionDto {

    @NotNull
    private StatutDemande statut;

    private String messageRefus;
}
