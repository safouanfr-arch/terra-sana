package com.example.demo.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.dto.RetraitCodeRequestDto;
import com.example.demo.dto.RetraitCommandeDto;
import com.example.demo.model.Commande;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetraitService {

    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;

    @Transactional(readOnly = true)
    public RetraitCommandeDto search(RetraitCodeRequestDto request) {
        String code = normalizeCode(request.getCodeRetrait());
        Commande commande = commandeRepository.findByCodeRetrait(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code de retrait inconnu."));
        return toWithdrawalDto(commande);
    }

    @Transactional
    public RetraitCommandeDto confirm(RetraitCodeRequestDto request) {
        String code = normalizeCode(request.getCodeRetrait());
        return toWithdrawalDto(commandeService.confirmWithdrawalByCode(code));
    }

    private String normalizeCode(String codeRetrait) {
        String normalized = codeRetrait != null ? codeRetrait.trim().toUpperCase(Locale.ROOT) : "";
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code de retrait est obligatoire.");
        }
        return normalized;
    }

    private RetraitCommandeDto toWithdrawalDto(Commande commande) {
        return RetraitCommandeDto.builder()
                .commandeId(commande.getId())
                .numeroCommande(commande.getNumero())
                .membreNom(commande.getMembre() != null ? commande.getMembre().getNom() : "")
                .membrePrenom(commande.getMembre() != null ? commande.getMembre().getPrenom() : "")
                .pointCollecte(commande.getPointCollecte() != null ? commande.getPointCollecte().getNom() : "")
                .dateRetrait(commande.getDateRetraitPrevu() != null
                        ? commande.getDateRetraitPrevu().toLocalDate()
                        : null)
                .heureDebut(commande.getCreneauCollecte() != null
                        ? commande.getCreneauCollecte().getHeureDebut()
                        : null)
                .heureFin(commande.getCreneauCollecte() != null
                        ? commande.getCreneauCollecte().getHeureFin()
                        : null)
                .montantTotal(commande.getMontantTTC())
                .statut(commande.getStatut())
                .paye(commande.getPaye())
                .codeRetrait(commande.getCodeRetrait())
                .build();
    }

    private RetraitCommandeDto toWithdrawalDto(CommandeResponseDto commande) {
        return RetraitCommandeDto.builder()
                .commandeId(commande.getId())
                .numeroCommande(commande.getNumero())
                .membreNom(commande.getMembre() != null ? commande.getMembre().getNom() : "")
                .membrePrenom(commande.getMembre() != null ? commande.getMembre().getPrenom() : "")
                .pointCollecte(commande.getPointCollecte() != null ? commande.getPointCollecte().getNom() : "")
                .dateRetrait(commande.getDateRetrait() != null ? commande.getDateRetrait().toLocalDate() : null)
                .heureDebut(commande.getCreneauRetrait() != null
                        ? commande.getCreneauRetrait().getHeureDebut()
                        : null)
                .heureFin(commande.getCreneauRetrait() != null
                        ? commande.getCreneauRetrait().getHeureFin()
                        : null)
                .montantTotal(commande.getMontantTTC())
                .statut(commande.getStatut())
                .paye(commande.getPaye())
                .codeRetrait(commande.getCodeRetrait())
                .build();
    }
}
