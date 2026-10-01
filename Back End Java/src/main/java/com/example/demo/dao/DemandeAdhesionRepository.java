package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.enums.StatutDemande;
import com.example.demo.model.DemandeAdhesion;

import jakarta.persistence.LockModeType;

public interface DemandeAdhesionRepository extends JpaRepository<DemandeAdhesion, Long> {

    boolean existsByEmailIgnoreCaseAndStatut(String email, StatutDemande statut);
    boolean existsByEmailIgnoreCaseAndStatutIn(String email, List<StatutDemande> statuts);
    boolean existsByEmailActifNormalise(String emailActifNormalise);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DemandeAdhesion d where d.confirmationEmailTokenHash = :tokenHash")
    Optional<DemandeAdhesion> findByConfirmationEmailTokenHashForUpdate(
            @Param("tokenHash") String confirmationEmailTokenHash
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from DemandeAdhesion d
            where d.emailActifNormalise = :email
              and d.statut = :statut
            """)
    Optional<DemandeAdhesion> findActiveByEmailForUpdate(
            @Param("email") String emailActifNormalise,
            @Param("statut") StatutDemande statut
    );
    long countByStatut(StatutDemande statut);
    long countByStatutIn(List<StatutDemande> statuts);

    List<DemandeAdhesion> findAllByOrderByDateSoumissionDesc();
    List<DemandeAdhesion> findAllByStatutInOrderByDateSoumissionDesc(List<StatutDemande> statuts);
}
