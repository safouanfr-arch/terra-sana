package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.DemandeAdhesionCreateDto;
import com.example.demo.dto.DemandeAdhesionDecisionDto;
import com.example.demo.dto.DemandeAdhesionDto;
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

    @Mock
    private EmailConfirmationTokenService emailConfirmationTokenService;

    @InjectMocks
    private DemandeAdhesionService demandeAdhesionService;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 10, 9, 0);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW.atZone(ZoneId.of("Europe/Brussels")).toInstant(),
                ZoneId.of("Europe/Brussels")
        );
        ReflectionTestUtils.setField(demandeAdhesionService, "clock", clock);
        ReflectionTestUtils.setField(demandeAdhesionService, "frontendBaseUrl", "https://terra.test");
        ReflectionTestUtils.setField(demandeAdhesionService, "conditionsVersion", "v1.0");
    }

    @Test
    void createStoresOnlyTokenHashAndWaitsForEmailConfirmation() {
        String rawToken = "raw-confirmation-token";
        String tokenHash = "a".repeat(64);
        when(membreRepository.findByEmailIgnoreCase("marie@terra.test")).thenReturn(Optional.empty());
        when(demandeAdhesionRepository.existsByEmailActifNormalise("marie@terra.test")).thenReturn(false);
        when(demandeAdhesionRepository.existsByEmailIgnoreCaseAndStatutIn(eq("marie@terra.test"), any()))
                .thenReturn(false);
        when(emailConfirmationTokenService.generate())
                .thenReturn(new EmailConfirmationTokenService.GeneratedToken(rawToken, tokenHash));
        when(demandeAdhesionRepository.saveAndFlush(any(DemandeAdhesion.class))).thenAnswer(invocation -> {
            DemandeAdhesion demande = invocation.getArgument(0);
            demande.setId(20L);
            return demande;
        });

        DemandeAdhesionDto response = demandeAdhesionService.create(createRequest(" Marie@Terra.Test "));

        assertThat(response.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        verify(demandeAdhesionRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(demande ->
                demande.getStatut() == StatutDemande.A_CONFIRMER
                        && "marie@terra.test".equals(demande.getEmailActifNormalise())
                        && tokenHash.equals(demande.getConfirmationEmailTokenHash())
                        && !rawToken.equals(demande.getConfirmationEmailTokenHash())
                        && NOW.plusHours(24).equals(demande.getConfirmationEmailExpiresAt())
                        && NOW.equals(demande.getDateDernierEnvoiConfirmation())
                        && NOW.equals(demande.getConditionsAccepteesAt())
                        && "v1.0".equals(demande.getConditionsVersion())
        ));
        assertThat(response.getConditionsAccepteesAt()).isEqualTo(NOW);
        assertThat(response.getConditionsVersion()).isEqualTo("v1.0");
        verify(generatedEmailService).generateMembershipEmailConfirmation(
                any(DemandeAdhesion.class),
                contains("https://terra.test/confirmation-adhesion?token=" + rawToken)
        );
        verify(generatedEmailService, never()).generateMembershipRequestReceived(any());
    }

    @Test
    void createRejectsConditionsThatAreNotAccepted() {
        DemandeAdhesionCreateDto request = createRequest("marie@terra.test");
        request.setAccepteConditions(false);

        assertThatThrownBy(() -> demandeAdhesionService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(demandeAdhesionRepository, never()).saveAndFlush(any());
        verify(generatedEmailService, never()).generateMembershipEmailConfirmation(any(), any());
    }

    @Test
    void createRejectsExistingMember() {
        when(membreRepository.findByEmailIgnoreCase("marie@terra.test"))
                .thenReturn(Optional.of(Membre.builder().id(1L).email("marie@terra.test").build()));

        assertThatThrownBy(() -> demandeAdhesionService.create(createRequest("marie@terra.test")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void createRejectsExistingActiveRequest() {
        when(membreRepository.findByEmailIgnoreCase("marie@terra.test")).thenReturn(Optional.empty());
        when(demandeAdhesionRepository.existsByEmailActifNormalise("marie@terra.test")).thenReturn(true);

        assertThatThrownBy(() -> demandeAdhesionService.create(createRequest("marie@terra.test")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void validConfirmationIsIdempotentAndReceiptEmailFailureKeepsPendingStatus() {
        String rawToken = "valid-token";
        String tokenHash = "b".repeat(64);
        DemandeAdhesion demande = pendingConfirmation(tokenHash, NOW.plusHours(1));
        when(emailConfirmationTokenService.hash(rawToken)).thenReturn(tokenHash);
        when(demandeAdhesionRepository.findByConfirmationEmailTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(demande));
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService).generateMembershipRequestReceived(demande);

        var first = demandeAdhesionService.confirmEmail(rawToken);
        var second = demandeAdhesionService.confirmEmail(rawToken);

        assertThat(first.getStatut()).isEqualTo("CONFIRMED");
        assertThat(second.getStatut()).isEqualTo("ALREADY_CONFIRMED");
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
        assertThat(demande.getDateConfirmationEmail()).isEqualTo(NOW);
        assertThat(demande.getConfirmationEmailTokenHash()).isEqualTo(tokenHash);
        assertThat(demande.getEmailActifNormalise()).isEqualTo("marie@terra.test");
        assertThat(demande.getConditionsAccepteesAt()).isEqualTo(NOW.minusDays(1));
        assertThat(demande.getConditionsVersion()).isEqualTo("v1.0");
        verify(generatedEmailService, times(1)).generateMembershipRequestReceived(demande);
    }

    @Test
    void resendRotatesTokenAndOnlyNewTokenCanConfirmOnceEvenWhenEmailGenerationFails() {
        String oldRawToken = "old-token";
        String oldHash = "1".repeat(64);
        String newRawToken = "new-token";
        String newHash = "2".repeat(64);
        DemandeAdhesion demande = pendingConfirmation(oldHash, NOW.minusMinutes(1));
        demande.setDateDernierEnvoiConfirmation(NOW.minusMinutes(5));

        when(demandeAdhesionRepository.findActiveByEmailForUpdate(
                "marie@terra.test",
                StatutDemande.A_CONFIRMER
        )).thenReturn(Optional.of(demande));
        when(emailConfirmationTokenService.generate())
                .thenReturn(new EmailConfirmationTokenService.GeneratedToken(newRawToken, newHash));
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService)
                .generateMembershipEmailConfirmation(eq(demande), contains(newRawToken));

        var resendResult = demandeAdhesionService.resendConfirmation(" Marie@Terra.Test ");

        assertThat(resendResult.getMessage()).contains("Si une demande a confirmer existe");
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        assertThat(demande.getConfirmationEmailTokenHash()).isEqualTo(newHash).isNotEqualTo(oldHash);
        assertThat(demande.getConfirmationEmailExpiresAt()).isEqualTo(NOW.plusHours(24));
        assertThat(demande.getDateDernierEnvoiConfirmation()).isEqualTo(NOW);

        when(emailConfirmationTokenService.hash(oldRawToken)).thenReturn(oldHash);
        when(emailConfirmationTokenService.hash(newRawToken)).thenReturn(newHash);
        when(demandeAdhesionRepository.findByConfirmationEmailTokenHashForUpdate(oldHash))
                .thenReturn(Optional.empty());
        when(demandeAdhesionRepository.findByConfirmationEmailTokenHashForUpdate(newHash))
                .thenReturn(Optional.of(demande));

        assertThat(demandeAdhesionService.confirmEmail(oldRawToken).getStatut()).isEqualTo("INVALID");
        assertThat(demandeAdhesionService.confirmEmail(newRawToken).getStatut()).isEqualTo("CONFIRMED");
        assertThat(demandeAdhesionService.confirmEmail(newRawToken).getStatut()).isEqualTo("ALREADY_CONFIRMED");
        verify(generatedEmailService, times(1)).generateMembershipRequestReceived(demande);
    }

    @Test
    void resendHonorsCooldownAndBecomesAvailableAtFiveMinuteBoundary() {
        DemandeAdhesion demande = pendingConfirmation("3".repeat(64), NOW.plusHours(1));
        demande.setDateDernierEnvoiConfirmation(NOW);
        when(demandeAdhesionRepository.findActiveByEmailForUpdate(
                "marie@terra.test",
                StatutDemande.A_CONFIRMER
        )).thenReturn(Optional.of(demande));

        var cooldownResponse = demandeAdhesionService.resendConfirmation("marie@terra.test");

        verify(emailConfirmationTokenService, never()).generate();
        assertThat(cooldownResponse.getMessage()).contains("Si une demande a confirmer existe");

        Clock afterCooldown = Clock.fixed(
                NOW.plusMinutes(5).atZone(ZoneId.of("Europe/Brussels")).toInstant(),
                ZoneId.of("Europe/Brussels")
        );
        ReflectionTestUtils.setField(demandeAdhesionService, "clock", afterCooldown);
        when(emailConfirmationTokenService.generate())
                .thenReturn(new EmailConfirmationTokenService.GeneratedToken("fresh-token", "4".repeat(64)));
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);

        demandeAdhesionService.resendConfirmation("marie@terra.test");

        verify(emailConfirmationTokenService).generate();
        assertThat(demande.getDateDernierEnvoiConfirmation()).isEqualTo(NOW.plusMinutes(5));
    }

    @Test
    void resendUnknownAddressReturnsSameGenericResponseWithoutSendingEmail() {
        when(demandeAdhesionRepository.findActiveByEmailForUpdate(
                "unknown@terra.test",
                StatutDemande.A_CONFIRMER
        )).thenReturn(Optional.empty());

        var result = demandeAdhesionService.resendConfirmation("unknown@terra.test");

        assertThat(result.getMessage()).contains("Si une demande a confirmer existe");
        verify(generatedEmailService, never()).generateMembershipEmailConfirmation(any(), any());
    }

    @Test
    void invalidTokenReturnsCleanResult() {
        when(emailConfirmationTokenService.hash("unknown")).thenReturn("c".repeat(64));
        when(demandeAdhesionRepository.findByConfirmationEmailTokenHashForUpdate("c".repeat(64)))
                .thenReturn(Optional.empty());

        var result = demandeAdhesionService.confirmEmail("unknown");

        assertThat(result.getStatut()).isEqualTo("INVALID");
        verify(generatedEmailService, never()).generateMembershipRequestReceived(any());
    }

    @Test
    void expiredTokenLeavesRequestAwaitingConfirmation() {
        String tokenHash = "d".repeat(64);
        DemandeAdhesion demande = pendingConfirmation(tokenHash, NOW);
        when(emailConfirmationTokenService.hash("expired")).thenReturn(tokenHash);
        when(demandeAdhesionRepository.findByConfirmationEmailTokenHashForUpdate(tokenHash))
                .thenReturn(Optional.of(demande));

        var result = demandeAdhesionService.confirmEmail("expired");

        assertThat(result.getStatut()).isEqualTo("EXPIRED");
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        assertThat(demande.getDateConfirmationEmail()).isNull();
        verify(demandeAdhesionRepository, never()).save(any());
        verify(generatedEmailService, never()).generateMembershipRequestReceived(any());
    }

    @Test
    void adminCannotDecideRequestAwaitingEmailConfirmation() {
        DemandeAdhesion demande = pendingConfirmation("e".repeat(64), NOW.plusHours(1));
        when(demandeAdhesionRepository.findById(30L)).thenReturn(Optional.of(demande));

        assertThatThrownBy(() -> demandeAdhesionService.decide(
                30L,
                DemandeAdhesionDecisionDto.builder().statut(StatutDemande.APPROUVEE).build(),
                1L
        )).isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void adminListRequestsOnlyAdministrativeStatuses() {
        when(demandeAdhesionRepository.findAllByStatutInOrderByDateSoumissionDesc(any()))
                .thenReturn(java.util.List.of());

        demandeAdhesionService.getAll();

        verify(demandeAdhesionRepository).findAllByStatutInOrderByDateSoumissionDesc(
                java.util.List.of(
                        StatutDemande.EN_ATTENTE,
                        StatutDemande.APPROUVEE,
                        StatutDemande.REFUSEE
                )
        );
    }

    @Test
    void rejectionReleasesActiveEmailAndClearsConfirmationToken() {
        DemandeAdhesion demande = DemandeAdhesion.builder()
                .id(40L)
                .email("marie@terra.test")
                .statut(StatutDemande.EN_ATTENTE)
                .emailActifNormalise("marie@terra.test")
                .confirmationEmailTokenHash("f".repeat(64))
                .confirmationEmailExpiresAt(NOW.plusHours(1))
                .dateConfirmationEmail(NOW.minusMinutes(5))
                .conditionsAccepteesAt(NOW.minusDays(1))
                .conditionsVersion("v1.0")
                .build();
        Administrateur admin = Administrateur.builder().Id(1L).email("admin@terra.test").build();
        when(demandeAdhesionRepository.findById(40L)).thenReturn(Optional.of(demande));
        when(administrateurRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);

        DemandeAdhesionDto result = demandeAdhesionService.decide(
                40L,
                DemandeAdhesionDecisionDto.builder()
                        .statut(StatutDemande.REFUSEE)
                        .messageRefus("Hors perimetre")
                        .build(),
                1L
        );

        assertThat(result.getStatut()).isEqualTo(StatutDemande.REFUSEE);
        assertThat(demande.getEmailActifNormalise()).isNull();
        assertThat(demande.getConfirmationEmailTokenHash()).isNull();
        assertThat(demande.getConfirmationEmailExpiresAt()).isNull();
        assertThat(demande.getDateConfirmationEmail()).isEqualTo(NOW.minusMinutes(5));
        assertThat(result.getConditionsAccepteesAt()).isEqualTo(NOW.minusDays(1));
        assertThat(result.getConditionsVersion()).isEqualTo("v1.0");
    }

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
                .conditionsAccepteesAt(NOW.minusDays(1))
                .conditionsVersion("v1.0")
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
        AuthService.IssuedPasswordToken token = new AuthService.IssuedPasswordToken(
                "http://localhost:5173/creation-mot-de-passe/demo",
                LocalDateTime.of(2026, 5, 2, 9, 0)
        );

        when(demandeAdhesionRepository.findById(10L)).thenReturn(Optional.of(demande));
        when(administrateurRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(membreRepository.findByEmailIgnoreCase("marie@terra.test")).thenReturn(Optional.of(membre));
        when(authService.issuePasswordTokenForMember(membre)).thenReturn(token);
        when(demandeAdhesionRepository.save(demande)).thenReturn(demande);
        doThrow(new RuntimeException("SMTP indisponible"))
                .when(generatedEmailService)
                .generateMembershipValidation(demande, token.passwordLink());

        DemandeAdhesionDto response = demandeAdhesionService.decide(
                10L,
                DemandeAdhesionDecisionDto.builder()
                        .statut(StatutDemande.APPROUVEE)
                        .build(),
                1L
        );

        assertThat(response.getStatut()).isEqualTo(StatutDemande.APPROUVEE);
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.APPROUVEE);
        verify(generatedEmailService).generateMembershipValidation(demande, token.passwordLink());
        assertThat(demande.getEmailActifNormalise()).isNull();
        assertThat(demande.getConfirmationEmailTokenHash()).isNull();
        assertThat(response.getConditionsAccepteesAt()).isEqualTo(NOW.minusDays(1));
        assertThat(response.getConditionsVersion()).isEqualTo("v1.0");
    }

    private DemandeAdhesionCreateDto createRequest(String email) {
        return DemandeAdhesionCreateDto.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .accepteConditions(true)
                .build();
    }

    private DemandeAdhesion pendingConfirmation(String tokenHash, LocalDateTime expiresAt) {
        return DemandeAdhesion.builder()
                .id(30L)
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@terra.test")
                .telephone("0470000000")
                .adresse("Rue Test")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(StatutDemande.A_CONFIRMER)
                .emailActifNormalise("marie@terra.test")
                .confirmationEmailTokenHash(tokenHash)
                .confirmationEmailExpiresAt(expiresAt)
                .conditionsAccepteesAt(NOW.minusDays(1))
                .conditionsVersion("v1.0")
                .build();
    }
}
