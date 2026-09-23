package com.example.demo.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.ConfigurationEmail;

public interface ConfigurationEmailRepository extends JpaRepository<ConfigurationEmail, Long> {
}
