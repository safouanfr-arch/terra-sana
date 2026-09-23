package com.example.demo.dao;

import com.example.demo.model.PointCollecte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointCollecteRepository extends JpaRepository<PointCollecte, Long> {

    List<PointCollecte> findByActifTrue();
}
