package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.AdminDashboardStatsDto;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.StatutDemande;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final MembreRepository membreRepository;
    private final DemandeAdhesionRepository demandeAdhesionRepository;
    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final PointCollecteRepository pointCollecteRepository;
    private final CommandeRepository commandeRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return AdminDashboardStatsDto.builder()
                .membresTotal(membreRepository.count())
                .membresSuspendus(membreRepository.countByActifFalse())
                .demandesTotal(demandeAdhesionRepository.count())
                .demandesEnAttente(demandeAdhesionRepository.countByStatut(StatutDemande.EN_ATTENTE))
                .produitsTotal(produitRepository.count())
                .produitsActifs(produitRepository.countByActifTrue())
                .categoriesTotal(categorieRepository.count())
                .pointsCollecteTotal(pointCollecteRepository.count())
                .commandesTotal(commandeRepository.count())
                .commandesJour(commandeRepository.countByDateCommandeBetween(startOfDay, endOfDay))
                .commandesConfirmed(commandeRepository.countByStatut(StatutCommande.CONFIRMED))
                .commandesInPrep(commandeRepository.countByStatut(StatutCommande.IN_PREP))
                .commandesReady(commandeRepository.countByStatut(StatutCommande.READY))
                .commandesDistributed(commandeRepository.countByStatut(StatutCommande.DISTRIBUTED))
                .commandesCancelled(commandeRepository.countByStatut(StatutCommande.CANCELLED))
                .lowStock(produitRepository.countByStockActuelLessThanEqual(BigDecimal.valueOf(5)))
                .build();
    }
}
