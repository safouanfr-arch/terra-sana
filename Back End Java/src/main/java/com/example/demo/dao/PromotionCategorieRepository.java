package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.PromotionCategorie;

public interface PromotionCategorieRepository extends JpaRepository<PromotionCategorie, Long> {
    void deleteByPromotion_Id(Long promotionId);
}
