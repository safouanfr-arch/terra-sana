package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.enums.StatutDemande;
import com.example.demo.model.DemandeAdhesion;

public interface DemandeAdhesionRepository extends JpaRepository<DemandeAdhesion, Long> {

    boolean existsByEmailIgnoreCaseAndStatut(String email, StatutDemande statut);
    long countByStatut(StatutDemande statut);

    List<DemandeAdhesion> findAllByOrderByDateSoumissionDesc();
}
