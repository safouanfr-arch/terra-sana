package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.model.enums.StatutCommande;

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
public class CommandeResponseDto {

    private Long id;
    private String numero;
    private String codeRetrait;
    private StatutCommande statut;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private Boolean paye;
    private LocalDateTime datePaiement;
    private LocalDateTime dateCommande;
    private LocalDateTime dateRetrait;
    private LocalDateTime dateAnnulation;
    private String commentaire;
    private CommandeMembreResumeDto membre;
    private CommandePointCollecteResumeDto pointCollecte;
    private CommandeCreneauResumeDto creneauRetrait;
    private List<CommandeProduitDto> produits;
}
