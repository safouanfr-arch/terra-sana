package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.DemandeAdhesionCreateDto;
import com.example.demo.dto.DemandeAdhesionDecisionDto;
import com.example.demo.dto.DemandeAdhesionDto;
import com.example.demo.dto.MembreDto;
import com.example.demo.dto.PasswordTokenResponseDto;
import com.example.demo.mapper.DemandeAdhesionMapper;
import com.example.demo.model.Administrateur;
import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.StatutDemande;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DemandeAdhesionService {

    private static final Logger log = LoggerFactory.getLogger(DemandeAdhesionService.class);

    private final DemandeAdhesionRepository demandeAdhesionRepository;
    private final MembreRepository membreRepository;
    private final AdministrateurRepository administrateurRepository;
    private final MembreService membreService;
    private final AuthService authService;
    private final GeneratedEmailService generatedEmailService;

    @Transactional
    public DemandeAdhesionDto create(DemandeAdhesionCreateDto dto) {
        String email = normalize(dto.getEmail());
        if (membreRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un membre existe deja avec cette adresse email.");
        }
        if (demandeAdhesionRepository.existsByEmailIgnoreCaseAndStatut(email, StatutDemande.EN_ATTENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande d'adhesion est deja en attente pour cette adresse email.");
        }

        DemandeAdhesion entity = DemandeAdhesionMapper.toEntity(dto);
        entity.setEmail(email);
        entity.setPays(normalizePays(dto.getPays()));
        entity.setMessage(trimToNull(dto.getMessage()));
        entity.setMessageRefus(null);
        entity.setStatut(StatutDemande.EN_ATTENTE);
        DemandeAdhesion saved = demandeAdhesionRepository.save(entity);
        safeEmailGeneration(() -> generatedEmailService.generateMembershipRequestReceived(saved), "demande adhesion recue " + saved.getEmail());
        return DemandeAdhesionMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeAdhesionDto> getAll() {
        return demandeAdhesionRepository.findAllByOrderByDateSoumissionDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DemandeAdhesionDto decide(Long id, DemandeAdhesionDecisionDto dto, Long administrateurId) {
        DemandeAdhesion demande = demandeAdhesionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande d'adhesion introuvable."));

        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a deja ete traitee.");
        }

        Administrateur administrateur = administrateurRepository.findById(administrateurId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Administrateur introuvable."));

        if (dto.getStatut() == StatutDemande.APPROUVEE) {
            Membre membre = createMemberIfMissing(demande);
            PasswordTokenResponseDto passwordToken = authService.issuePasswordTokenForMember(
                    membre,
                    "Lien de creation de mot de passe genere."
            );
            demande.approuver(administrateur);
            DemandeAdhesionDto response = toDto(demandeAdhesionRepository.save(demande));
            response.setLienActivation(passwordToken.getResetLink());
            safeEmailGeneration(
                    () -> generatedEmailService.generateMembershipValidation(demande, passwordToken.getResetLink()),
                    "validation adhesion " + demande.getEmail()
            );
            return response;
        } else if (dto.getStatut() == StatutDemande.REFUSEE) {
            demande.refuser(administrateur, trimToNull(dto.getMessageRefus()));
            safeEmailGeneration(() -> generatedEmailService.generateMembershipRejection(demande), "refus adhesion " + demande.getEmail());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut doit etre APPROUVEE ou REFUSEE.");
        }

        return toDto(demandeAdhesionRepository.save(demande));
    }

    private Membre createMemberIfMissing(DemandeAdhesion demande) {
        Membre existing = membreRepository.findByEmailIgnoreCase(demande.getEmail()).orElse(null);
        if (existing != null) {
            return existing;
        }

        String placeholderPassword = "Init#" + java.util.UUID.randomUUID();

        MembreDto created = membreService.create(MembreDto.builder()
                .nom(demande.getNom())
                .prenom(demande.getPrenom())
                .email(demande.getEmail())
                .telephone(demande.getTelephone())
                .adresse(demande.getAdresse())
                .ville(demande.getVille())
                .codePostal(demande.getCodePostal())
                .pays(normalizePays(demande.getPays()))
                .motDePasse(placeholderPassword)
                .actif(true)
                .build());

        return membreRepository.findById(created.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre cree introuvable."));
    }

    private DemandeAdhesionDto toDto(DemandeAdhesion demande) {
        DemandeAdhesionDto dto = DemandeAdhesionMapper.toDto(demande);
        dto.setLienActivation(authService.getActivePasswordLinkForEmail(demande.getEmail()));
        return dto;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizePays(String value) {
        String pays = trimToNull(value);
        return pays != null ? pays : "Belgique";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void safeEmailGeneration(RunnableWithException action, String context) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Generation d'email ignoree pour {}: {}", context, exception.getMessage());
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }
}
