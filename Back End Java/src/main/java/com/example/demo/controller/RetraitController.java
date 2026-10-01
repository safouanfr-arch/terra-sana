package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.RetraitCodeRequestDto;
import com.example.demo.dto.RetraitCommandeDto;
import com.example.demo.service.RetraitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/retraits")
@RequiredArgsConstructor
public class RetraitController {

    private final RetraitService retraitService;

    @PostMapping("/rechercher")
    public RetraitCommandeDto search(@Valid @RequestBody RetraitCodeRequestDto request) {
        return retraitService.search(request);
    }

    @PostMapping("/confirmer")
    public RetraitCommandeDto confirm(@Valid @RequestBody RetraitCodeRequestDto request) {
        return retraitService.confirm(request);
    }
}
