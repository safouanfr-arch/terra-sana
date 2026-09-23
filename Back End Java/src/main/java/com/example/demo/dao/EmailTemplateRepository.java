package com.example.demo.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.EmailTemplate;
import com.example.demo.model.enums.EmailNotificationType;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, EmailNotificationType> {

    List<EmailTemplate> findAllByOrderByTypeAsc();
}
