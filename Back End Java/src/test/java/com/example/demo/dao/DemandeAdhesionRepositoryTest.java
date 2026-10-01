package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.enums.StatutDemande;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DemandeAdhesionRepositoryTest {

    @Autowired
    private DemandeAdhesionRepository repository;

    @ParameterizedTest
    @EnumSource(value = StatutDemande.class, names = {"A_CONFIRMER", "EN_ATTENTE"})
    void databaseConstraintPreventsTwoActiveRequestsForSameNormalizedEmail(StatutDemande firstStatus) {
        repository.saveAndFlush(request("Duplicate@Terra.Test", firstStatus, "a".repeat(64)));

        assertThatThrownBy(() -> repository.saveAndFlush(request(
                " duplicate@terra.test ",
                StatutDemande.A_CONFIRMER,
                "b".repeat(64)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectedHistoryReleasesEmailForANewActiveRequest() {
        repository.saveAndFlush(request("retry@terra.test", StatutDemande.REFUSEE, null));

        DemandeAdhesion active = repository.saveAndFlush(request(
                "retry@terra.test",
                StatutDemande.A_CONFIRMER,
                "c".repeat(64)
        ));

        assertThat(active.getId()).isNotNull();
        assertThat(active.getEmailActifNormalise()).isEqualTo("retry@terra.test");
    }

    private DemandeAdhesion request(String email, StatutDemande status, String tokenHash) {
        return DemandeAdhesion.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(status)
                .confirmationEmailTokenHash(tokenHash)
                .confirmationEmailExpiresAt(tokenHash != null ? LocalDateTime.now().plusHours(24) : null)
                .build();
    }
}
