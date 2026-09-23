package com.example.demo.controller;

import com.example.demo.dto.PointCollecteDto;
import com.example.demo.service.PointCollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/points-collecte")
@RequiredArgsConstructor
public class PointCollecteController {

    private final PointCollecteService pointCollecteService;

    @PostMapping
    public PointCollecteDto create(@Valid @RequestBody PointCollecteDto dto) {
        return pointCollecteService.create(dto);
    }

    @GetMapping
    public List<PointCollecteDto> getAll() {
        return pointCollecteService.getAll();
    }

    @GetMapping("/actifs")
    public List<PointCollecteDto> getAllActifs() {
        return pointCollecteService.getAllActifs();
    }

    @PutMapping("/{id}")
    public PointCollecteDto update(@PathVariable(name="id") Long id, @Valid @RequestBody PointCollecteDto dto) {
        return pointCollecteService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable(name="id") Long id) {
        pointCollecteService.delete(id);
    }
}
