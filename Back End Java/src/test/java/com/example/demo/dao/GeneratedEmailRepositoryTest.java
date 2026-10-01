package com.example.demo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.EmailSendStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GeneratedEmailRepositoryTest {

    @Autowired
    private GeneratedEmailRepository repository;

    @Test
    void preparationEmailTypeAndSlotAreUniqueInDatabase() {
        repository.saveAndFlush(email(7001L));

        assertThat(repository.findByTypeAndCreneauCollecteId(
                EmailNotificationType.PREPARATION_DOSSIER,
                7001L
        )).isPresent();
        assertThatThrownBy(() -> repository.saveAndFlush(email(7001L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private GeneratedEmail email(Long creneauId) {
        return GeneratedEmail.builder()
                .type(EmailNotificationType.PREPARATION_DOSSIER)
                .creneauCollecteId(creneauId)
                .destinataire("preparation@terrasana.test")
                .sujet("Dossier")
                .contenuHtml("<p>Dossier</p>")
                .statutEnvoi(EmailSendStatus.PENDING)
                .build();
    }
}
