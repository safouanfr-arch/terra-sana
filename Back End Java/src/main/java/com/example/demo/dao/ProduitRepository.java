package com.example.demo.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Produit;
import com.example.demo.model.enums.UniteProduit;

import jakarta.persistence.LockModeType;

public interface ProduitRepository extends JpaRepository<Produit, Long> {

	List<Produit> findByActifTrue();
	long countByActifTrue();
	long countByActifFalse();
	long countByStockActuelLessThanEqual(BigDecimal stockActuel);
	long countByCategorie_Id(Long categorieId);
	boolean existsByNomAndUniteAndCategorieId(String nom, UniteProduit unite, Long categorieId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Produit p where p.Id = :id")
	java.util.Optional<Produit> findByIdForUpdate(@Param("id") Long id);
}
