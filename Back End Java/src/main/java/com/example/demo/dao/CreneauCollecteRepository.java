package com.example.demo.dao;

import com.example.demo.model.CreneauCollecte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreneauCollecteRepository extends JpaRepository<CreneauCollecte, Long> {

    List<CreneauCollecte> findByPointCollecte_Id(Long pointCollecteId);
}
