package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.PromotionProduit;

public interface PromotionProduitRepository extends JpaRepository<PromotionProduit, Long> {
    void deleteByPromotion_Id(Long promotionId);
}
