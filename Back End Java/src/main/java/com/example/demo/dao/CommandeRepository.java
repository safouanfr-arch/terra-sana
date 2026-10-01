package com.example.demo.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Commande;
import com.example.demo.model.enums.StatutCommande;

import jakarta.persistence.LockModeType;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
   List<Commande> findAllByOrderByDateCommandeDesc();
   List<Commande> findByMembre_IdOrderByDateCommandeDesc(Long membreId);
   long countByStatut(StatutCommande statut);
   long countByMembre_Id(Long membreId);
   long countByDateCommandeBetween(LocalDateTime start, LocalDateTime end);
   boolean existsByCodeRetrait(String codeRetrait);
   Optional<Commande> findByCodeRetrait(String codeRetrait);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select c from Commande c where c.id = :id")
   Optional<Commande> findByIdForUpdate(@Param("id") Long id);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select c from Commande c where c.codeRetrait = :codeRetrait")
   Optional<Commande> findByCodeRetraitForUpdate(@Param("codeRetrait") String codeRetrait);

   @Query("""
           select c.id from Commande c
           where c.statut = :status
             and c.dateRetraitPrevu > :nowExclusive
             and c.dateRetraitPrevu <= :deadlineInclusive
           order by c.dateRetraitPrevu asc, c.id asc
           """)
   List<Long> findPreparationCandidateIds(
           @Param("nowExclusive") LocalDateTime nowExclusive,
           @Param("deadlineInclusive") LocalDateTime deadlineInclusive,
           @Param("status") StatutCommande status
   );

   @Query("""
           select c.id from Commande c
           where c.creneauCollecte.id = :creneauId
           order by c.dateCommande asc, c.id asc
           """)
   List<Long> findIdsByCreneauCollecteId(@Param("creneauId") Long creneauId);

   @EntityGraph(attributePaths = {
           "membre",
           "creneauCollecte",
           "creneauCollecte.pointCollecte",
           "lignes",
           "lignes.produit",
           "lignes.produit.categorie"
   })
   List<Commande> findByCreneauCollecte_IdAndStatutInOrderByNumeroAsc(
           Long creneauId,
           List<StatutCommande> statuses
   );

   long countByCreneauCollecte_IdAndStatutIn(Long creneauId, List<StatutCommande> statuses);

   @Query("select c.creneauCollecte.id from Commande c where c.id = :commandeId")
   Optional<Long> findCreneauCollecteIdByCommandeId(@Param("commandeId") Long commandeId);

   @Query("""
           select c.id from Commande c
           where c.dateRetraitPrevu >= :startInclusive
             and c.dateRetraitPrevu < :endExclusive
             and c.statut in :statuses
           """)
   List<Long> findReminderCandidateIds(
           @Param("startInclusive") LocalDateTime startInclusive,
           @Param("endExclusive") LocalDateTime endExclusive,
           @Param("statuses") List<StatutCommande> statuses
   );
}
