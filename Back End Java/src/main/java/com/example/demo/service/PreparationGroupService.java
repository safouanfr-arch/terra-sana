package com.example.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dto.PreparationGroupFinalizationDto;
import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.service.CommandeService.ConditionalTransitionResult;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PreparationGroupService {

    private final CreneauCollecteRepository creneauCollecteRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;
    private final PreparationPdfService preparationPdfService;

    public PreparationGroupFinalizationDto finalizeGroup(Long creneauId) {
        if (!creneauCollecteRepository.existsById(creneauId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Creneau de collecte introuvable.");
        }

        List<Long> commandeIds = commandeRepository.findIdsByCreneauCollecteId(creneauId);
        int finalized = 0;
        int excludedForIncident = 0;
        int ignored = 0;

        for (Long commandeId : commandeIds) {
            ConditionalTransitionResult result = commandeService.transitionStatusIfCurrent(
                    commandeId,
                    StatutCommande.IN_PREP,
                    StatutCommande.READY
            );
            switch (result) {
                case TRANSITIONED -> finalized++;
                case BLOCKED_BY_INCIDENT -> excludedForIncident++;
                case NOT_ELIGIBLE -> ignored++;
            }
        }

        return PreparationGroupFinalizationDto.builder()
                .creneauId(creneauId)
                .commandesFinalisees(finalized)
                .commandesExcluesIncident(excludedForIncident)
                .commandesIgnoreesNonEligibles(ignored)
                .build();
    }

    @Transactional(readOnly = true)
    public PreparationPdfDocument generatePdf(Long creneauId) {
        CreneauCollecte creneau = creneauCollecteRepository.findById(creneauId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Creneau de collecte introuvable."
                ));
        return preparationPdfService.generate(
                creneau,
                commandeRepository.findByCreneauCollecte_IdAndStatutInOrderByNumeroAsc(
                        creneauId,
                        List.of(StatutCommande.IN_PREP, StatutCommande.READY)
                )
        );
    }
}
