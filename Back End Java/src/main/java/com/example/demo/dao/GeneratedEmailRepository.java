package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.enums.EmailNotificationType;

public interface GeneratedEmailRepository extends JpaRepository<GeneratedEmail, Long> {

    List<GeneratedEmail> findAllByOrderByCreatedAtDesc();

    boolean existsByTypeAndCommandeId(EmailNotificationType type, Long commandeId);
}
