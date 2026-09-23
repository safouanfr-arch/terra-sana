package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.MouvementStock;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, String> {
   
}