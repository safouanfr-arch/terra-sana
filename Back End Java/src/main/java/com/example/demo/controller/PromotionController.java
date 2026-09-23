package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.PromotionDto;
import com.example.demo.service.PromotionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public List<PromotionDto> getAll() {
        return promotionService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromotionDto create(@Valid @RequestBody PromotionDto dto) {
        return promotionService.create(dto);
    }

    @PutMapping("/{id}")
    public PromotionDto update(@PathVariable("id") Long id, @Valid @RequestBody PromotionDto dto) {
        return promotionService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        promotionService.delete(id);
    }
}
