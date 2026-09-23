package com.example.demo.mapper;

import java.time.LocalTime;
import java.util.List;

import com.example.demo.dto.CommandeCreneauResumeDto;
import com.example.demo.dto.CommandeMembreResumeDto;
import com.example.demo.dto.CommandePointCollecteResumeDto;
import com.example.demo.dto.CommandeProduitDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;

public class CommandeMapper {

    public static CommandeResponseDto toDto(Commande entity) {
        return CommandeResponseDto.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .codeRetrait(entity.getCodeRetrait())
                .statut(entity.getStatut())
                .montantHT(entity.getMontantHT())
                .montantTVA(entity.getMontantTVA())
                .montantTTC(entity.getMontantTTC())
                .paye(entity.getPaye())
                .datePaiement(entity.getDatePaiement())
                .dateCommande(entity.getDateCommande())
                .dateRetrait(entity.getDateRetraitPrevu())
                .dateAnnulation(entity.getDateAnnulation())
                .commentaire(entity.getCommentaire())
                .membre(toMembreDto(entity.getMembre()))
                .pointCollecte(toPointDto(entity.getPointCollecte()))
                .creneauRetrait(toCreneauDto(entity.getCreneauCollecte()))
                .produits(toProduitsDto(entity.getLignes()))
                .build();
    }

    private static CommandeMembreResumeDto toMembreDto(Membre membre) {
        if (membre == null) {
            return null;
        }

        return CommandeMembreResumeDto.builder()
                .id(membre.getId())
                .nom(membre.getNom())
                .prenom(membre.getPrenom())
                .email(membre.getEmail())
                .build();
    }

    private static CommandePointCollecteResumeDto toPointDto(PointCollecte pointCollecte) {
        if (pointCollecte == null) {
            return null;
        }

        return CommandePointCollecteResumeDto.builder()
                .id(pointCollecte.getId())
                .nom(pointCollecte.getNom())
                .adresse(pointCollecte.getAdresse())
                .build();
    }

    private static CommandeCreneauResumeDto toCreneauDto(CreneauCollecte creneau) {
        if (creneau == null) {
            return null;
        }

        return CommandeCreneauResumeDto.builder()
                .id(creneau.getId())
                .pointCollecteId(creneau.getPointCollecte() != null ? creneau.getPointCollecte().getId() : null)
                .date(creneau.getCreneau())
                .heureDebut(creneau.getHeureDebut())
                .heureFin(creneau.getHeureFin())
                .horaire(formatHoraire(creneau.getHeureDebut(), creneau.getHeureFin()))
                .build();
    }

    private static List<CommandeProduitDto> toProduitsDto(List<LigneCommande> lignes) {
        return lignes.stream()
                .map(CommandeMapper::toProduitDto)
                .toList();
    }

    private static CommandeProduitDto toProduitDto(LigneCommande ligne) {
        Produit produit = ligne.getProduit();

        return CommandeProduitDto.builder()
                .id(ligne.getId())
                .produitId(produit != null ? produit.getId() : null)
                .nom(produit != null ? produit.getNom() : "")
                .unite(produit != null && produit.getUnite() != null ? produit.getUnite().name() : null)
                .quantite(ligne.getQuantite())
                .prixUnitaire(ligne.getPrixUnitaire())
                .prixUnitaireOriginal(ligne.getPrixUnitaireOriginal())
                .reductionUnitaire(ligne.getReductionUnitaire())
                .promotionId(ligne.getPromotionId())
                .promotionNom(ligne.getPromotionNom())
                .tauxTVA(ligne.getTauxTVA())
                .montantHT(ligne.getMontantHT())
                .montantTTC(ligne.getMontantTTC())
                .build();
    }

    private static String formatHoraire(LocalTime heureDebut, LocalTime heureFin) {
        if (heureDebut == null || heureFin == null) {
            return "";
        }
        return formatTime(heureDebut) + "-" + formatTime(heureFin);
    }

    private static String formatTime(LocalTime value) {
        return value.toString().length() >= 5 ? value.toString().substring(0, 5) : value.toString();
    }
}
