package com.example.demo.controller;

import com.example.demo.dto.CreneauCollecteDto;
import com.example.demo.service.CreneauCollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creneaux-collecte")
@RequiredArgsConstructor
public class CreneauCollecteController {

    private final CreneauCollecteService creneauCollecteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreneauCollecteDto create(@Valid @RequestBody CreneauCollecteDto dto) {
        return creneauCollecteService.create(dto);
    }

    @PutMapping("/{id}")
    public CreneauCollecteDto update(@PathVariable("id") Long id,
                                     @Valid @RequestBody CreneauCollecteDto dto) {
        return creneauCollecteService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        creneauCollecteService.delete(id);
    }
}
