package com.example.demo.controller;

import com.example.demo.dto.ProduitDto;
import com.example.demo.dto.ProduitStockAdjustmentDto;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.ProduitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produits")
@RequiredArgsConstructor

public class ProduitController {
	private final ProduitService produitService;
	
	@PostMapping
	public ProduitDto create(@RequestBody @Valid ProduitDto dto) {
		return produitService.create(dto);
	}
	@DeleteMapping("/{id}")
	public void delete(@PathVariable(name= "id") Long id) {
		produitService.delete(id);
	}
	@PutMapping("/{id}")
	public ProduitDto update(@PathVariable(name= "id") Long id,@RequestBody @Valid ProduitDto dto) {
		return produitService.update(id,dto);
	}
	@PutMapping("/{id}/stock")
	public ProduitDto adjustStock(
			@PathVariable(name = "id") Long id,
			@RequestBody @Valid ProduitStockAdjustmentDto dto,
			@AuthenticationPrincipal AppUserPrincipal principal
	) {
		return produitService.adjustStock(id, dto, principal.getId());
	}
	@GetMapping("/{id}")
	public ProduitDto getById(@PathVariable(name= "id") Long id) {
		return produitService.getById(id);
	}
	@GetMapping
	public List<ProduitDto> getAll() {
		return produitService.getAll();
	}
	@GetMapping("/actives")
	public List<ProduitDto> getAllActives() {
		return produitService.getAllActives();
	}
}
