package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CommandeCreateRequestDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.dto.CommandeStatutUpdateDto;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.CommandeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
public class CommandeController {

    private final CommandeService commandeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeResponseDto create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CommandeCreateRequestDto dto
    ) {
        return commandeService.createForMember(principal.getId(), dto);
    }

    @GetMapping
    public List<CommandeResponseDto> getAll() {
        return commandeService.getAll();
    }

    @GetMapping("/moi")
    public List<CommandeResponseDto> getMyOrders(@AuthenticationPrincipal AppUserPrincipal principal) {
        return commandeService.getByMembreId(principal.getId());
    }

    @PutMapping("/{id}/annuler")
    public CommandeResponseDto cancel(@PathVariable("id") Long id, @AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal.isAdmin()) {
            return commandeService.cancel(id);
        }
        return commandeService.cancelForMembre(id, principal.getId());
    }

    @PutMapping("/{id}/statut")
    public CommandeResponseDto updateStatus(@PathVariable("id") Long id, @Valid @RequestBody CommandeStatutUpdateDto dto) {
        return commandeService.updateStatus(id, dto);
    }
}
