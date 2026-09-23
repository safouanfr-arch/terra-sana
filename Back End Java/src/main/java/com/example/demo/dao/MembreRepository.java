package com.example.demo.dao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Membre;


public interface MembreRepository extends JpaRepository<Membre, Long> {

	boolean existsByEmail(String email);
	Optional<Membre> findByEmailIgnoreCase(String email);
	Optional<Membre> findByTokenMotDePasse(String tokenMotDePasse);
	long countByActifTrue();
	long countByActifFalse();

	List<Membre> findByActifTrue();
	List<Membre> findByActifFalse();
}
