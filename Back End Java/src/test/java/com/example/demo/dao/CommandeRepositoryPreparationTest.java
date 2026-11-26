package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.StatutCommande;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CommandeRepositoryPreparationTest {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private CreneauCollecteRepository creneauCollecteRepository;

    @Test
    void preparationCandidatesRespectTimeBoundariesAndStatus() {
        LocalDateTime now = LocalDateTime.of(2040, 5, 10, 10, 0);
        Membre membre = saveTestMember();
        CreneauCollecte creneau = creneauCollecteRepository.findAll().get(0);

        Commande after30Hours = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now.plusHours(30));
        Commande after23Hours50 = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now.plusHours(23).plusMinutes(50));
        Commande after6Hours = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now.plusHours(6));
        Commande exactly24Hours = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now.plusHours(24));
        Commande past = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now.minusMinutes(1));
        Commande exactlyNow = saveCommande(membre, creneau, StatutCommande.CONFIRMED, now);
        Commande inPrep = saveCommande(membre, creneau, StatutCommande.IN_PREP, now.plusHours(6));
        Commande ready = saveCommande(membre, creneau, StatutCommande.READY, now.plusHours(6));
        Commande distributed = saveCommande(membre, creneau, StatutCommande.DISTRIBUTED, now.plusHours(6));
        Commande cancelled = saveCommande(membre, creneau, StatutCommande.CANCELLED, now.plusHours(6));

        List<Long> candidates = commandeRepository.findPreparationCandidateIds(
                now,
                now.plusHours(24),
                StatutCommande.CONFIRMED
        );

        assertThat(candidates).containsExactlyInAnyOrder(
                after23Hours50.getId(),
                after6Hours.getId(),
                exactly24Hours.getId()
        );
        assertThat(candidates).doesNotContain(
                after30Hours.getId(),
                past.getId(),
                exactlyNow.getId(),
                inPrep.getId(),
                ready.getId(),
                distributed.getId(),
                cancelled.getId()
        );
    }

    @Test
    void withdrawalCodeIsUniqueAndCanBeRetrieved() {
        Membre membre = saveTestMember();
        CreneauCollecte creneau = creneauCollecteRepository.findAll().get(0);
        LocalDateTime retrait = LocalDateTime.of(2040, 5, 10, 14, 0);
        Commande first = buildCommande(membre, creneau, StatutCommande.READY, retrait, "UNIQ01");
        commandeRepository.saveAndFlush(first);

        assertThat(commandeRepository.existsByCodeRetrait("UNIQ01")).isTrue();
        assertThat(commandeRepository.findByCodeRetrait("UNIQ01")).contains(first);

        Commande duplicate = buildCommande(membre, creneau, StatutCommande.READY, retrait, "UNIQ01");
        assertThatThrownBy(() -> commandeRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Commande saveCommande(
            Membre membre,
            CreneauCollecte creneau,
            StatutCommande statut,
            LocalDateTime retrait
    ) {
        return commandeRepository.saveAndFlush(buildCommande(
                membre,
                creneau,
                statut,
                retrait,
                null
        ));
    }

    private Membre saveTestMember() {
        return membreRepository.saveAndFlush(Membre.builder()
                .nom("Membre")
                .prenom("Test")
                .email("member-" + UUID.randomUUID() + "@terra.test")
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .motDePasse("mot-de-passe-inutilise")
                .actif(true)
                .build());
    }

    private Commande buildCommande(
            Membre membre,
            CreneauCollecte creneau,
            StatutCommande statut,
            LocalDateTime retrait,
            String codeRetrait
    ) {
        return Commande.builder()
                .numero("TEST-PREP-" + UUID.randomUUID())
                .codeRetrait(codeRetrait)
                .statut(statut)
                .montantHT(BigDecimal.ZERO)
                .montantTVA(BigDecimal.ZERO)
                .montantTTC(BigDecimal.ZERO)
                .paye(false)
                .dateCommande(LocalDateTime.of(2040, 5, 1, 8, 0))
                .dateRetraitPrevu(retrait)
                .membre(membre)
                .creneauCollecte(creneau)
                .build();
    }
}
