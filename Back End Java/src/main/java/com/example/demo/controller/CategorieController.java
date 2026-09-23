package com.example.demo.controller;

import com.example.demo.dto.CategorieDto;
import com.example.demo.service.CategorieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    @PostMapping
    public CategorieDto create(@RequestBody @Valid CategorieDto dto) {
        return categorieService.create(dto);
    }

    @GetMapping
    public List<CategorieDto> getAll() {
        return categorieService.getAll();
    }
    
	@GetMapping("/actives")
	public List<CategorieDto> getAllActives(){
		return categorieService.getAllActives();
	}
	
    @PutMapping("/{id}")
    public CategorieDto update(@PathVariable(name= "id") Long id, @RequestBody @Valid CategorieDto dto) {
        return categorieService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable(name="id") Long id) {
        categorieService.delete(id);
    }
    
}
