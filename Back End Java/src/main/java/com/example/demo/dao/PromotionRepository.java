package com.example.demo.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.model.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findAllByOrderByDateDebutDesc();

    @Query("select p from Promotion p where p.actif = true and p.dateDebut <= :now and p.dateFin >= :now")
    List<Promotion> findActiveAt(@Param("now") LocalDateTime now);
}
