package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.example.demo.model.enums.StatutCommande;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetraitCommandeDto {

    private Long commandeId;
    private String numeroCommande;
    private String membreNom;
    private String membrePrenom;
    private String pointCollecte;
    private LocalDate dateRetrait;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private BigDecimal montantTotal;
    private StatutCommande statut;
    private Boolean paye;
    private LocalDateTime datePaiement;
    private String codeRetrait;
}
