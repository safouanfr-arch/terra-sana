package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dto.MembreDto;
import com.example.demo.model.Membre;

@ExtendWith(MockitoExtension.class)
public class MembreServiceTest {

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MembreService membreService;

    @Test
    void getAllReturnsEveryMemberWhenStatusIsTous() {
        when(membreRepository.findAll()).thenReturn(List.of(
                membre(1L, true, "actif@terra.test"),
                membre(2L, false, "suspendu@terra.test")
        ));

        List<MembreDto> result = membreService.getAll("tous");

        assertThat(result).hasSize(2);
        verify(membreRepository).findAll();
    }

    @Test
    void getAllReturnsOnlyActifsWhenRequested() {
        when(membreRepository.findByActifTrue()).thenReturn(List.of(
                membre(1L, true, "actif@terra.test")
        ));

        List<MembreDto> result = membreService.getAll("actif");

        assertThat(result)
                .hasSize(1)
                .extracting(MembreDto::getActif)
                .containsOnly(true);
    }

    @Test
    void getAllReturnsOnlySuspendusWhenRequested() {
        when(membreRepository.findByActifFalse()).thenReturn(List.of(
                membre(2L, false, "suspendu@terra.test")
        ));

        List<MembreDto> result = membreService.getAll("suspendu");

        assertThat(result)
                .hasSize(1)
                .extracting(MembreDto::getActif)
                .containsOnly(false);
    }

    @Test
    void getAllRejectsUnknownStatus() {
        assertThatThrownBy(() -> membreService.getAll("archive"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Membre membre(Long id, boolean actif, String email) {
        return Membre.builder()
                .id(id)
                .nom("Demo")
                .prenom("User")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue de test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .motDePasse("secret")
                .actif(actif)
                .build();
    }
}
