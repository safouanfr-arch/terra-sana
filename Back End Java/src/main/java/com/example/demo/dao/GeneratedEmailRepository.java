package com.example.demo.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.enums.EmailNotificationType;

import jakarta.persistence.LockModeType;

public interface GeneratedEmailRepository extends JpaRepository<GeneratedEmail, Long> {

    List<GeneratedEmail> findAllByOrderByCreatedAtDesc();

    boolean existsByTypeAndCommandeId(EmailNotificationType type, Long commandeId);

    Optional<GeneratedEmail> findByTypeAndCreneauCollecteId(
            EmailNotificationType type,
            Long creneauCollecteId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from GeneratedEmail e
            where e.type = :type and e.creneauCollecteId = :creneauCollecteId
            """)
    Optional<GeneratedEmail> findByTypeAndCreneauCollecteIdForUpdate(
            @Param("type") EmailNotificationType type,
            @Param("creneauCollecteId") Long creneauCollecteId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from GeneratedEmail e where e.id = :id")
    Optional<GeneratedEmail> findByIdForUpdate(@Param("id") Long id);
}
