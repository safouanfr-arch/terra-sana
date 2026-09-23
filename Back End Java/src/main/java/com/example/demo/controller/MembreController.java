package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MembreDto;
import com.example.demo.service.MembreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/membres")
@RequiredArgsConstructor
public class MembreController {

    private final MembreService membreService;

    @GetMapping
    public List<MembreDto> getAll(@RequestParam(name = "statut", required = false) String statut) {
        return membreService.getAll(statut);
    }

    @PutMapping("/{id}")
    public MembreDto update(@PathVariable("id") Long id, @Valid @RequestBody MembreDto dto) {
        return membreService.update(id, dto);
    }
}
