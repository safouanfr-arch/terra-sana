package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.AuthProfileUpdateDto;
import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.PasswordChangeDto;
import com.example.demo.dto.PasswordResetCompleteDto;
import com.example.demo.dto.PasswordResetRequestDto;
import com.example.demo.dto.PasswordTokenResponseDto;
import com.example.demo.dto.PasswordTokenStatusDto;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Membre;
import com.example.demo.security.AppUserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AdministrateurRepository administrateurRepository;
    private final MembreRepository membreRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeneratedEmailService generatedEmailService;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public AppUserPrincipal authenticate(LoginRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        String rawPassword = dto.getMotDePasse();

        return administrateurRepository.findByEmailIgnoreCase(email)
                .map(admin -> authenticateAdministrateur(admin, rawPassword))
                .orElseGet(() -> membreRepository.findByEmailIgnoreCase(email)
                        .map(membre -> authenticateMembre(membre, rawPassword))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.")));
    }

    public AuthResponseDto toResponse(AppUserPrincipal principal) {
        return AuthResponseDto.builder()
                .id(principal.getId())
                .email(principal.getEmail())
                .nom(principal.getNom())
                .prenom(principal.getPrenom())
                .role(principal.getRole())
                .actif(principal.getActif())
                .telephone(principal.getTelephone())
                .adresse(principal.getAdresse())
                .ville(principal.getVille())
                .codePostal(principal.getCodePostal())
                .pays(principal.getPays())
                .build();
    }

    @Transactional
    public AppUserPrincipal updateProfile(AppUserPrincipal principal, AuthProfileUpdateDto dto) {
        if (!principal.isMembre()) {
            throw new AccessDeniedException("Seuls les membres peuvent modifier leur profil.");
        }

        Membre membre = membreRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable."));

        String normalizedEmail = normalizeEmail(dto.getEmail());
        membreRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(other -> !other.getId().equals(membre.getId()))
                .ifPresent(other -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse email est deja utilisee.");
                });

        administrateurRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresent(admin -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse email est deja utilisee.");
                });

        membre.setNom(dto.getNom().trim());
        membre.setPrenom(dto.getPrenom().trim());
        membre.setEmail(normalizedEmail);
        membre.setTelephone(trimToNull(dto.getTelephone()));
        membre.setAdresse(trimToNull(dto.getAdresse()));
        membre.setVille(trimToNull(dto.getVille()));
        membre.setCodePostal(trimToNull(dto.getCodePostal()));
        membre.setPays(defaultPays(dto.getPays()));

        return buildPrincipal(membreRepository.save(membre));
    }

    @Transactional
    public void changePassword(AppUserPrincipal principal, PasswordChangeDto dto) {
        if (!principal.isMembre()) {
            throw new AccessDeniedException("Seuls les membres peuvent modifier leur mot de passe.");
        }

        Membre membre = membreRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable."));

        if (!passwordMatches(dto.getCurrentPassword(), membre.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }

        validatePasswordStrength(dto.getNewPassword());
        membre.setMotDePasse(passwordEncoder.encode(dto.getNewPassword()));
        membre.effacerTokenMotDePasse();
        membreRepository.save(membre);
    }

    @Transactional
    public PasswordTokenResponseDto requestPasswordReset(PasswordResetRequestDto dto) {
        Membre membre = membreRepository.findByEmailIgnoreCase(normalizeEmail(dto.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun membre ne correspond a cette adresse email."));

        if (Boolean.FALSE.equals(membre.getActif())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce compte est suspendu.");
        }

        PasswordTokenResponseDto response = issuePasswordToken(membre, "Lien de reinitialisation genere.");
        safeEmailGeneration(() -> generatedEmailService.generatePasswordSetup(membre, response.getResetLink()), "reinitialisation mot de passe " + membre.getEmail());
        return response;
    }

    @Transactional(readOnly = true)
    public PasswordTokenStatusDto getPasswordTokenStatus(String token) {
        return membreRepository.findByTokenMotDePasse(token)
                .map(membre -> {
                    boolean valid = membre.getDateExpirationTokenMotDePasse() != null
                            && membre.getDateExpirationTokenMotDePasse().isAfter(LocalDateTime.now());

                    return PasswordTokenStatusDto.builder()
                            .valid(valid)
                            .email(membre.getEmail())
                            .expiresAt(membre.getDateExpirationTokenMotDePasse())
                            .build();
                })
                .orElseGet(() -> PasswordTokenStatusDto.builder()
                        .valid(false)
                        .build());
    }

    @Transactional
    public void completePasswordReset(String token, PasswordResetCompleteDto dto) {
        Membre membre = membreRepository.findByTokenMotDePasse(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lien de creation de mot de passe introuvable."));

        if (membre.getDateExpirationTokenMotDePasse() == null
                || !membre.getDateExpirationTokenMotDePasse().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ce lien a expire.");
        }

        validatePasswordStrength(dto.getMotDePasse());
        membre.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        membre.effacerTokenMotDePasse();
        membre.setActif(true);
        membreRepository.save(membre);
    }

    @Transactional
    public PasswordTokenResponseDto issuePasswordTokenForMember(Membre membre, String message) {
        return issuePasswordToken(membre, message);
    }

    @Transactional(readOnly = true)
    public String getActivePasswordLinkForEmail(String email) {
        return membreRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(membre -> membre.getTokenMotDePasse() != null
                        && membre.getDateExpirationTokenMotDePasse() != null
                        && membre.getDateExpirationTokenMotDePasse().isAfter(LocalDateTime.now()))
                .map(membre -> buildPasswordLink(membre.getTokenMotDePasse()))
                .orElse(null);
    }

    private AppUserPrincipal authenticateAdministrateur(Administrateur admin, String rawPassword) {
        if (Boolean.FALSE.equals(admin.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte administrateur est desactive.");
        }

        if (!passwordMatches(rawPassword, admin.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }

        if (requiresMigration(admin.getMotDePasse())) {
            admin.setMotDePasse(passwordEncoder.encode(rawPassword));
        }
        admin.setDateConnexion(LocalDateTime.now());
        administrateurRepository.save(admin);

        return AppUserPrincipal.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .nom(admin.getNom())
                .prenom(admin.getPrenom())
                .role("admin")
                .actif(admin.getActif())
                .build();
    }

    private AppUserPrincipal authenticateMembre(Membre membre, String rawPassword) {
        if (Boolean.FALSE.equals(membre.getActif())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte membre est suspendu. Contactez un administrateur pour le reactiver.");
        }

        if (!passwordMatches(rawPassword, membre.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }

        if (requiresMigration(membre.getMotDePasse())) {
            membre.setMotDePasse(passwordEncoder.encode(rawPassword));
        }
        membre.setDateLogin(LocalDateTime.now());
        membreRepository.save(membre);

        return buildPrincipal(membre);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (requiresMigration(storedPassword)) {
            return rawPassword.equals(storedPassword);
        }

        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    private boolean requiresMigration(String storedPassword) {
        return storedPassword == null
                || !(storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$"));
    }

    private AppUserPrincipal buildPrincipal(Membre membre) {
        return AppUserPrincipal.builder()
                .id(membre.getId())
                .email(membre.getEmail())
                .nom(membre.getNom())
                .prenom(membre.getPrenom())
                .role("membre")
                .actif(membre.getActif())
                .telephone(membre.getTelephone())
                .adresse(membre.getAdresse())
                .ville(membre.getVille())
                .codePostal(membre.getCodePostal())
                .pays(membre.getPays())
                .build();
    }

    private PasswordTokenResponseDto issuePasswordToken(Membre membre, String message) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiration = LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS);
        membre.definirTokenMotDePasse(token, expiration);
        membreRepository.save(membre);

        return PasswordTokenResponseDto.builder()
                .email(membre.getEmail())
                .resetLink(buildPasswordLink(token))
                .expiresAt(expiration)
                .message(message)
                .build();
    }

    private String buildPasswordLink(String token) {
        String normalizedBase = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return normalizedBase + "/creation-mot-de-passe/" + token;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caracteres.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins une majuscule.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins une minuscule.");
        }
        if (!password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins un chiffre.");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins un caractere special.");
        }
    }

    private String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultPays(String pays) {
        String normalized = trimToNull(pays);
        return normalized != null ? normalized : "Belgique";
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
