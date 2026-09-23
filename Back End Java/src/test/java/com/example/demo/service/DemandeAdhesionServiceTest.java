package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.DemandeAdhesionDecisionDto;
import com.example.demo.dto.DemandeAdhesionDto;
import com.example.demo.dto.PasswordTokenResponseDto;
import com.example.demo.model.Administrateur;
import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.StatutDemande;

@ExtendWith(MockitoExtension.class)
class DemandeAdhesionServiceTest {

    @Mock
    private DemandeAdhesionRepository demandeAdhesionRepository;

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private AdministrateurRepository administrateurRepository;

    @Mock
    private MembreService membreService;

    @Mock
    private AuthService authService;

    @Mock
    private GeneratedEmailService generatedEmailService;

    @InjectMocks
    private DemandeAdhesionService demandeAdhesionService;

    @Test
    void approveMembershipKeepsDecisionWhenEmailGenerationFails() {
        DemandeAdhesion demande = DemandeAdhesion.builder()
                .id(10L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .telephone("0470000000")
                .adresse("Rue Test")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(StatutDemande.EN_ATTENTE)
                .dateSoumission(LocalDateTime.of(2026, 5, 1, 9, 0))
                .build();
        Administrateur admin = Administrateur.builder()
                .Id(1L)
                .email("admin@terra.test")
                .nom("Admin")
                .prenom("Demo")
                .actif(true)
                .build();
        Membre membre = Membre.builder()
                .id(4L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .actif(true)
                .build();
        PasswordTokenResponseDto token = PasswordTokenResponseDto.builder()
                .email("marie@terra.test")
                .resetLink("http://localhost:5173/creation-mot-de-passe/demo")
                .expiresAt(LocalDateTime.of(2026, 5, 2, 9, 0))
                .build();

        when(demandeAdhesionRepository.findById(10L)).thenReturn(Optional.of(demande));
        when(administrateurRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(membreRepository.findByEmailIgnoreCase("marie@terra.test")).thenReturn(Optional.of(membre));
        when(authService.issuePasswordTokenForMember(eq(membre), any())).thenReturn(token);
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);
        when(authService.getActivePasswordLinkForEmail("marie@terra.test")).thenReturn(token.getResetLink());
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService)
                .generateMembershipValidation(demande, token.getResetLink());

        DemandeAdhesionDto response = demandeAdhesionService.decide(
                10L,
                DemandeAdhesionDecisionDto.builder()
                        .statut(StatutDemande.APPROUVEE)
                        .build(),
                1L
        );

        assertThat(response.getStatut()).isEqualTo(StatutDemande.APPROUVEE);
        assertThat(response.getLienActivation()).isEqualTo(token.getResetLink());
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.APPROUVEE);
        verify(generatedEmailService).generateMembershipValidation(demande, token.getResetLink());
    }
}
