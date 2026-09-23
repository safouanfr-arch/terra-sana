package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.MembreDto;
import com.example.demo.mapper.MembreMapper;
import com.example.demo.model.Membre;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final MembreRepository membreRepository;
    private final CommandeRepository commandeRepository;
    private final PasswordEncoder passwordEncoder;

    public MembreDto create(MembreDto dto) {
        if (membreRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un membre a deja cette adresse email.");
        }
        if (dto.getMotDePasse() == null || dto.getMotDePasse().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe est requis pour creer un membre.");
        }

        Membre entity = MembreMapper.toEntity(dto);
        entity.setMotDePasse(encodeIfNeeded(dto.getMotDePasse()));
        entity.setDateCreation(LocalDateTime.now());
        entity.setDateLogin(LocalDateTime.now());

        return toDto(membreRepository.save(entity));
    }

    public List<MembreDto> getAll() {
        return membreRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<MembreDto> getAll(String statut) {
        if (statut == null || statut.isBlank() || "tous".equalsIgnoreCase(statut)) {
            return getAll();
        }

        List<Membre> membres = switch (statut.toLowerCase()) {
            case "actif" -> membreRepository.findByActifTrue();
            case "suspendu" -> membreRepository.findByActifFalse();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide : " + statut);
        };

        return membres.stream()
                .map(this::toDto)
                .toList();
    }

    public MembreDto update(Long id, MembreDto dto) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        if (dto.getEmail() != null && !dto.getEmail().equals(membre.getEmail()) && membreRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un autre membre utilise deja cette adresse email.");
        }

        MembreMapper.updateEntityFromDto(membre, dto);
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank()) {
            membre.setMotDePasse(encodeIfNeeded(dto.getMotDePasse()));
        }

        return toDto(membreRepository.save(membre));
    }

    private MembreDto toDto(Membre membre) {
        MembreDto dto = MembreMapper.toDto(membre);
        dto.setNbCommandes(membre.getId() == null ? 0L : commandeRepository.countByMembre_Id(membre.getId()));
        return dto;
    }

    private String encodeIfNeeded(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            return motDePasse;
        }
        if (motDePasse.startsWith("$2a$") || motDePasse.startsWith("$2b$") || motDePasse.startsWith("$2y$")) {
            return motDePasse;
        }
        return passwordEncoder.encode(motDePasse);
    }
}
