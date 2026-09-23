package com.example.demo.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
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

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("select c from Commande c where c.id = :id")
   Optional<Commande> findByIdForUpdate(@Param("id") Long id);

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
