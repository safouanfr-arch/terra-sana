package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.PasswordResetCompleteDto;
import com.example.demo.dto.PasswordResetRequestDto;
import com.example.demo.model.Membre;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordResetTest {

    private static final String RAW_TOKEN = "raw-password-token";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String PUBLIC_MESSAGE =
            "Si un compte correspond à cette adresse, un email de réinitialisation a été envoyé.";

    @Mock
    private AdministrateurRepository administrateurRepository;

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GeneratedEmailService generatedEmailService;

    @Mock
    private PasswordTokenService passwordTokenService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:5173");
    }

    @Test
    void existingMemberGetsGenericResponseWhileOnlyHashIsStoredAndRawLinkIsEmailed() {
        Membre membre = activeMember();
        when(membreRepository.findByEmailIgnoreCase("membre@terra.test")).thenReturn(Optional.of(membre));
        when(passwordTokenService.generate())
                .thenReturn(new PasswordTokenService.GeneratedToken(RAW_TOKEN, TOKEN_HASH));

        var response = authService.requestPasswordReset(request("MEMBRE@terra.test"));

        assertThat(response.getMessage()).isEqualTo(PUBLIC_MESSAGE);
        assertThat(membre.getTokenMotDePasse()).isEqualTo(TOKEN_HASH).isNotEqualTo(RAW_TOKEN);
        assertThat(membre.getDateExpirationTokenMotDePasse()).isAfter(LocalDateTime.now().plusHours(23));
        verify(generatedEmailService).generatePasswordSetup(
                membre,
                "http://localhost:5173/creation-mot-de-passe/" + RAW_TOKEN
        );
    }

    @Test
    void unknownAndInactiveEmailsReceiveTheSameGenericResponseWithoutEmailGeneration() {
        Membre inactive = activeMember();
        inactive.setActif(false);
        when(membreRepository.findByEmailIgnoreCase("unknown@terra.test")).thenReturn(Optional.empty());
        when(membreRepository.findByEmailIgnoreCase("inactive@terra.test")).thenReturn(Optional.of(inactive));

        var unknownResponse = authService.requestPasswordReset(request("unknown@terra.test"));
        var inactiveResponse = authService.requestPasswordReset(request("inactive@terra.test"));

        assertThat(unknownResponse.getMessage()).isEqualTo(PUBLIC_MESSAGE);
        assertThat(inactiveResponse.getMessage()).isEqualTo(PUBLIC_MESSAGE);
        verifyNoInteractions(passwordTokenService, generatedEmailService);
    }

    @Test
    void smtpFailureNeverChangesThePublicResponseOrExposesTheToken() {
        Membre membre = activeMember();
        when(membreRepository.findByEmailIgnoreCase("membre@terra.test")).thenReturn(Optional.of(membre));
        when(passwordTokenService.generate())
                .thenReturn(new PasswordTokenService.GeneratedToken(RAW_TOKEN, TOKEN_HASH));
        when(generatedEmailService.generatePasswordSetup(membre, "http://localhost:5173/creation-mot-de-passe/" + RAW_TOKEN))
                .thenThrow(new RuntimeException("SMTP indisponible"));

        var response = authService.requestPasswordReset(request("membre@terra.test"));

        assertThat(response.getMessage()).isEqualTo(PUBLIC_MESSAGE).doesNotContain(RAW_TOKEN, "http");
    }

    @Test
    void tokenStatusHashesRawTokenBeforeLookup() {
        Membre membre = activeMember();
        membre.definirTokenMotDePasse(TOKEN_HASH, LocalDateTime.now().plusHours(1));
        when(passwordTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(membreRepository.findByTokenMotDePasse(TOKEN_HASH)).thenReturn(Optional.of(membre));

        var status = authService.getPasswordTokenStatus(RAW_TOKEN);

        assertThat(status.isValid()).isTrue();
        assertThat(status.getEmail()).isEqualTo("membre@terra.test");
        verify(membreRepository).findByTokenMotDePasse(TOKEN_HASH);
        verify(membreRepository, never()).findByTokenMotDePasse(RAW_TOKEN);
    }

    @Test
    void validTokenChangesPasswordAndCannotBeUsedTwice() {
        Membre membre = activeMember();
        membre.definirTokenMotDePasse(TOKEN_HASH, LocalDateTime.now().plusHours(1));
        when(passwordTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(membreRepository.findByTokenMotDePasse(TOKEN_HASH))
                .thenReturn(Optional.of(membre), Optional.empty());
        when(passwordEncoder.encode("Nouveau1!")).thenReturn("bcrypt-password");

        authService.completePasswordReset(RAW_TOKEN, password("Nouveau1!"));

        assertThat(membre.getMotDePasse()).isEqualTo("bcrypt-password");
        assertThat(membre.getTokenMotDePasse()).isNull();
        assertThat(membre.getDateExpirationTokenMotDePasse()).isNull();

        assertThatThrownBy(() -> authService.completePasswordReset(RAW_TOKEN, password("Nouveau1!")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void invalidAndExpiredTokensAreRejected() {
        Membre expired = activeMember();
        expired.definirTokenMotDePasse(TOKEN_HASH, LocalDateTime.now().minusMinutes(1));
        when(passwordTokenService.hash("invalid")).thenReturn("b".repeat(64));
        when(passwordTokenService.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(membreRepository.findByTokenMotDePasse("b".repeat(64))).thenReturn(Optional.empty());
        when(membreRepository.findByTokenMotDePasse(TOKEN_HASH)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.completePasswordReset("invalid", password("Nouveau1!")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> authService.completePasswordReset(RAW_TOKEN, password("Nouveau1!")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void initialPasswordDefinitionUsesTheSameHashedOneTimeTokenMechanism() {
        Membre membre = activeMember();
        when(passwordTokenService.generate())
                .thenReturn(new PasswordTokenService.GeneratedToken(RAW_TOKEN, TOKEN_HASH));

        AuthService.IssuedPasswordToken issued = authService.issuePasswordTokenForMember(membre);

        assertThat(issued.passwordLink()).endsWith("/creation-mot-de-passe/" + RAW_TOKEN);
        assertThat(membre.getTokenMotDePasse()).isEqualTo(TOKEN_HASH).isNotEqualTo(RAW_TOKEN);
        verify(membreRepository).save(membre);
    }

    private PasswordResetRequestDto request(String email) {
        return PasswordResetRequestDto.builder().email(email).build();
    }

    private PasswordResetCompleteDto password(String value) {
        return PasswordResetCompleteDto.builder().motDePasse(value).build();
    }

    private Membre activeMember() {
        return Membre.builder()
                .id(5L)
                .nom("Dupont")
                .prenom("Marie")
                .email("membre@terra.test")
                .motDePasse("ancien")
                .actif(true)
                .build();
    }
}
