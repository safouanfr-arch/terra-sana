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

import com.example.demo.dto.DemandeAdhesionCreateDto;
import com.example.demo.dto.DemandeAdhesionConfirmationRequestDto;
import com.example.demo.dto.DemandeAdhesionConfirmationResponseDto;
import com.example.demo.dto.DemandeAdhesionDecisionDto;
import com.example.demo.dto.DemandeAdhesionDto;
import com.example.demo.dto.DemandeAdhesionResendRequestDto;
import com.example.demo.dto.DemandeAdhesionResendResponseDto;
import com.example.demo.security.AppUserPrincipal;
import com.example.demo.service.DemandeAdhesionService;
import com.example.demo.service.PublicAdhesionRateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/demandes-adhesion")
@RequiredArgsConstructor
public class DemandeAdhesionController {

    private final DemandeAdhesionService demandeAdhesionService;
    private final PublicAdhesionRateLimitService publicAdhesionRateLimitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeAdhesionDto create(
            @Valid @RequestBody DemandeAdhesionCreateDto dto,
            HttpServletRequest request
    ) {
        publicAdhesionRateLimitService.check(request.getRemoteAddr(), dto.getEmail());
        return demandeAdhesionService.create(dto);
    }

    @PostMapping("/confirmation")
    public DemandeAdhesionConfirmationResponseDto confirmEmail(
            @Valid @RequestBody DemandeAdhesionConfirmationRequestDto dto
    ) {
        return demandeAdhesionService.confirmEmail(dto.getToken());
    }

    @PostMapping("/confirmation/renvoyer")
    public DemandeAdhesionResendResponseDto resendConfirmation(
            @Valid @RequestBody DemandeAdhesionResendRequestDto dto,
            HttpServletRequest request
    ) {
        publicAdhesionRateLimitService.check(request.getRemoteAddr(), dto.getEmail());
        return demandeAdhesionService.resendConfirmation(dto.getEmail());
    }

    @GetMapping
    public List<DemandeAdhesionDto> getAll() {
        return demandeAdhesionService.getAll();
    }

    @PutMapping("/{id}/decision")
    public DemandeAdhesionDto decide(
            @PathVariable("id") Long id,
            @Valid @RequestBody DemandeAdhesionDecisionDto dto,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return demandeAdhesionService.decide(id, dto, principal.getId());
    }
}
