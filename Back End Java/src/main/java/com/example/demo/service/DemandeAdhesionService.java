package com.example.demo.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.DemandeAdhesionCreateDto;
import com.example.demo.dto.DemandeAdhesionConfirmationResponseDto;
import com.example.demo.dto.DemandeAdhesionDecisionDto;
import com.example.demo.dto.DemandeAdhesionDto;
import com.example.demo.dto.DemandeAdhesionResendResponseDto;
import com.example.demo.dto.MembreDto;
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
    private static final int CONFIRMATION_VALIDITY_HOURS = 24;
    private static final int CONFIRMATION_RESEND_COOLDOWN_MINUTES = 5;
    private static final String RESEND_PUBLIC_MESSAGE =
            "Si une demande a confirmer existe pour cette adresse, un nouveau message de confirmation a ete envoye.";
    private static final List<StatutDemande> ACTIVE_STATUSES =
            List.of(StatutDemande.A_CONFIRMER, StatutDemande.EN_ATTENTE);
    private static final List<StatutDemande> ADMIN_STATUSES =
            List.of(StatutDemande.EN_ATTENTE, StatutDemande.APPROUVEE, StatutDemande.REFUSEE);

    private final DemandeAdhesionRepository demandeAdhesionRepository;
    private final MembreRepository membreRepository;
    private final AdministrateurRepository administrateurRepository;
    private final MembreService membreService;
    private final AuthService authService;
    private final GeneratedEmailService generatedEmailService;
    private final EmailConfirmationTokenService emailConfirmationTokenService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.membership.conditions-version}")
    private String conditionsVersion;

    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public DemandeAdhesionDto create(DemandeAdhesionCreateDto dto) {
        if (!Boolean.TRUE.equals(dto.getAccepteConditions())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les conditions doivent etre acceptees.");
        }

        String email = normalize(dto.getEmail());
        if (membreRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un membre existe deja avec cette adresse email.");
        }
        if (demandeAdhesionRepository.existsByEmailActifNormalise(email)
                || demandeAdhesionRepository.existsByEmailIgnoreCaseAndStatutIn(email, ACTIVE_STATUSES)) {
            throw activeRequestConflict();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        EmailConfirmationTokenService.GeneratedToken confirmationToken = emailConfirmationTokenService.generate();
        DemandeAdhesion entity = DemandeAdhesionMapper.toEntity(dto);
        entity.setEmail(email);
        entity.setEmailActifNormalise(email);
        entity.setPays(normalizePays(dto.getPays()));
        entity.setMessage(trimToNull(dto.getMessage()));
        entity.setMessageRefus(null);
        entity.setStatut(StatutDemande.A_CONFIRMER);
        entity.renouvelerConfirmation(
                confirmationToken.hash(),
                now.plusHours(CONFIRMATION_VALIDITY_HOURS),
                now
        );
        entity.setDateConfirmationEmail(null);
        entity.setConditionsAccepteesAt(now);
        entity.setConditionsVersion(conditionsVersion);

        DemandeAdhesion saved;
        try {
            saved = demandeAdhesionRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            throw activeRequestConflict();
        }

        String confirmationLink = buildConfirmationLink(confirmationToken.rawToken());
        safeEmailGeneration(
                () -> generatedEmailService.generateMembershipEmailConfirmation(saved, confirmationLink),
                "confirmation email adhesion " + saved.getEmail()
        );
        return DemandeAdhesionMapper.toDto(saved);
    }

    @Transactional
    public DemandeAdhesionResendResponseDto resendConfirmation(String rawEmail) {
        String email = normalize(rawEmail);
        DemandeAdhesion demande = demandeAdhesionRepository
                .findActiveByEmailForUpdate(email, StatutDemande.A_CONFIRMER)
                .orElse(null);
        if (demande == null) {
            return resendPublicResponse();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime lastSentAt = demande.getDateDernierEnvoiConfirmation();
        if (lastSentAt != null
                && now.isBefore(lastSentAt.plusMinutes(CONFIRMATION_RESEND_COOLDOWN_MINUTES))) {
            return resendPublicResponse();
        }

        EmailConfirmationTokenService.GeneratedToken confirmationToken = emailConfirmationTokenService.generate();
        demande.renouvelerConfirmation(
                confirmationToken.hash(),
                now.plusHours(CONFIRMATION_VALIDITY_HOURS),
                now
        );
        DemandeAdhesion saved = demandeAdhesionRepository.save(demande);
        String confirmationLink = buildConfirmationLink(confirmationToken.rawToken());
        safeEmailGeneration(
                () -> generatedEmailService.generateMembershipEmailConfirmation(saved, confirmationLink),
                "renvoi confirmation email adhesion " + saved.getEmail()
        );
        return resendPublicResponse();
    }

    @Transactional(readOnly = true)
    public List<DemandeAdhesionDto> getAll() {
        return demandeAdhesionRepository.findAllByStatutInOrderByDateSoumissionDesc(ADMIN_STATUSES)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DemandeAdhesionConfirmationResponseDto confirmEmail(String rawToken) {
        String tokenHash = emailConfirmationTokenService.hash(rawToken.trim());
        DemandeAdhesion demande = demandeAdhesionRepository
                .findByConfirmationEmailTokenHashForUpdate(tokenHash)
                .orElse(null);

        if (demande == null) {
            return confirmationResult("INVALID", "Ce lien de confirmation est invalide.");
        }
        if (demande.getStatut() == StatutDemande.EN_ATTENTE) {
            return confirmationResult(
                    "ALREADY_CONFIRMED",
                    "Votre adresse email a deja ete confirmee. Votre demande est en attente de traitement."
            );
        }
        if (demande.getStatut() != StatutDemande.A_CONFIRMER) {
            return confirmationResult("INVALID", "Ce lien de confirmation est invalide.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (demande.getConfirmationEmailExpiresAt() == null
                || !demande.getConfirmationEmailExpiresAt().isAfter(now)) {
            return confirmationResult("EXPIRED", "Ce lien de confirmation a expire.");
        }

        demande.confirmerEmail(now);
        DemandeAdhesion saved = demandeAdhesionRepository.save(demande);
        safeEmailGeneration(
                () -> generatedEmailService.generateMembershipRequestReceived(saved),
                "demande adhesion recue " + saved.getEmail()
        );
        return confirmationResult(
                "CONFIRMED",
                "Votre adresse email a ete confirmee. Votre demande a ete transmise a Terra Sana."
        );
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
            AuthService.IssuedPasswordToken passwordToken = authService.issuePasswordTokenForMember(membre);
            demande.approuver(administrateur);
            DemandeAdhesionDto response = toDto(demandeAdhesionRepository.save(demande));
            safeEmailGeneration(
                    () -> generatedEmailService.generateMembershipValidation(demande, passwordToken.passwordLink()),
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
        return DemandeAdhesionMapper.toDto(demande);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException activeRequestConflict() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Une demande d'adhesion active existe deja pour cette adresse email."
        );
    }

    private String buildConfirmationLink(String rawToken) {
        String normalizedBase = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return normalizedBase + "/confirmation-adhesion?token=" + rawToken;
    }

    private DemandeAdhesionConfirmationResponseDto confirmationResult(String status, String message) {
        return DemandeAdhesionConfirmationResponseDto.builder()
                .statut(status)
                .message(message)
                .build();
    }

    private DemandeAdhesionResendResponseDto resendPublicResponse() {
        return new DemandeAdhesionResendResponseDto(RESEND_PUBLIC_MESSAGE);
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
