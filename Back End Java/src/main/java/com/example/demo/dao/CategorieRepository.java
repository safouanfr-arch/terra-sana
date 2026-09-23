package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Categorie;
import java.util.List;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
	
	List<Categorie> findByActifTrue();

	boolean existsByNom(String nom);
}
