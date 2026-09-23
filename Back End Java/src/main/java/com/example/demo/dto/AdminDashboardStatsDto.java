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
public class AdminDashboardStatsDto {

    private long membresTotal;
    private long membresSuspendus;
    private long demandesTotal;
    private long demandesEnAttente;
    private long produitsTotal;
    private long produitsActifs;
    private long categoriesTotal;
    private long pointsCollecteTotal;
    private long commandesTotal;
    private long commandesJour;
    private long commandesConfirmed;
    private long commandesInPrep;
    private long commandesReady;
    private long commandesDistributed;
    private long commandesCancelled;
    private long lowStock;
}
