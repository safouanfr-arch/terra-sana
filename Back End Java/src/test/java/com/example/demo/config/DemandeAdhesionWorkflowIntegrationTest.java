package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.DemandeAdhesionRepository;
import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.model.DemandeAdhesion;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.StatutDemande;
import com.example.demo.service.EmailConfirmationTokenService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DemandeAdhesionWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemandeAdhesionRepository demandeRepository;

    @Autowired
    private GeneratedEmailRepository generatedEmailRepository;

    @Autowired
    private EmailConfirmationTokenService tokenService;

    @Autowired
    private MembreRepository membreRepository;

    @Test
    void publicCreationStaysHiddenAndStoresFailedConfirmationEmailWithoutRawToken() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@terra.test";
        long waitingBefore = demandeRepository.countByStatut(StatutDemande.EN_ATTENTE);
        long administrativeTotalBefore = demandeRepository.countByStatutIn(List.of(
                StatutDemande.EN_ATTENTE,
                StatutDemande.APPROUVEE,
                StatutDemande.REFUSEE
        ));
        LocalDateTime before = LocalDateTime.now();

        mockMvc.perform(post("/api/demandes-adhesion")
                        .with(remoteAddress("192.0.2.10"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("A_CONFIRMER"))
                .andExpect(jsonPath("$.confirmationEmailTokenHash").doesNotExist())
                .andExpect(jsonPath("$.confirmationEmailExpiresAt").doesNotExist());

        DemandeAdhesion demande = demandeRepository.findAll().stream()
                .filter(item -> email.equals(item.getEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(demande.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        assertThat(demande.getConfirmationEmailTokenHash()).hasSize(64);
        assertThat(demande.getConfirmationEmailExpiresAt())
                .isBetween(before.plusHours(24), LocalDateTime.now().plusHours(24).plusSeconds(2));
        assertThat(demande.getDateConfirmationEmail()).isNull();
        assertThat(demande.getEmailActifNormalise()).isEqualTo(email);
        assertThat(demande.getConditionsAccepteesAt()).isBetween(
                before,
                LocalDateTime.now().plusSeconds(2)
        );
        assertThat(demande.getConditionsVersion()).isEqualTo("v1.0");
        assertThat(demande.getDateDernierEnvoiConfirmation()).isBetween(
                before,
                LocalDateTime.now().plusSeconds(2)
        );

        List<GeneratedEmail> histories = generatedEmailRepository.findAll().stream()
                .filter(item -> demande.getId().equals(item.getDemandeAdhesionId()))
                .toList();
        assertThat(histories).singleElement().satisfies(history -> {
            assertThat(history.getType()).isEqualTo(EmailNotificationType.MEMBERSHIP_EMAIL_CONFIRMATION);
            assertThat(history.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
            assertThat(history.getContenuHtml()).doesNotContain("confirmation-adhesion?token=");
        });
        assertThat(histories).noneMatch(item ->
                item.getType() == EmailNotificationType.MEMBERSHIP_REQUEST_RECEIVED);

        String adminList = mockMvc.perform(get("/api/demandes-adhesion")
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(adminList).doesNotContain(email);

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demandesEnAttente").value(waitingBefore))
                .andExpect(jsonPath("$.demandesTotal").value(administrativeTotalBefore));
    }

    @Test
    void conditionsMustBePresentAndTrueWhileServerStoresAcceptanceProof() throws Exception {
        String absentEmail = "conditions-absent-" + UUID.randomUUID() + "@terra.test";
        String falseEmail = "conditions-false-" + UUID.randomUUID() + "@terra.test";
        String trueEmail = "conditions-true-" + UUID.randomUUID() + "@terra.test";

        mockMvc.perform(post("/api/demandes-adhesion")
                        .with(remoteAddress("192.0.2.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(absentEmail, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/demandes-adhesion")
                        .with(remoteAddress("192.0.2.12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(falseEmail, false)))
                .andExpect(status().isBadRequest());

        LocalDateTime before = LocalDateTime.now();
        mockMvc.perform(post("/api/demandes-adhesion")
                        .with(remoteAddress("192.0.2.13"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(trueEmail, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conditionsAccepteesAt").exists())
                .andExpect(jsonPath("$.conditionsVersion").value("v1.0"));

        DemandeAdhesion saved = demandeRepository.findAll().stream()
                .filter(item -> trueEmail.equals(item.getEmail()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getConditionsAccepteesAt())
                .isBetween(before, LocalDateTime.now().plusSeconds(2));
        assertThat(saved.getConditionsVersion()).isEqualTo("v1.0");
        assertThat(saved.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        assertThat(demandeRepository.findAll()).noneMatch(item ->
                absentEmail.equals(item.getEmail()) || falseEmail.equals(item.getEmail()));
    }

    @Test
    void confirmationEndpointIsPublicAndReturnsCleanInvalidResult() throws Exception {
        mockMvc.perform(post("/api/demandes-adhesion/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"unknown-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INVALID"))
                .andExpect(jsonPath("$.message").value("Ce lien de confirmation est invalide."));
    }

    @Test
    void validConfirmationIsIdempotentAndFailedReceiptEmailKeepsRequestPending() throws Exception {
        String rawToken = "known-confirmation-token";
        String email = "confirmed-" + UUID.randomUUID() + "@terra.test";
        DemandeAdhesion demande = demandeRepository.saveAndFlush(DemandeAdhesion.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(StatutDemande.A_CONFIRMER)
                .confirmationEmailTokenHash(tokenService.hash(rawToken))
                .confirmationEmailExpiresAt(LocalDateTime.now().plusHours(1))
                .conditionsAccepteesAt(LocalDateTime.now().minusDays(1))
                .conditionsVersion("v1.0")
                .build());

        mockMvc.perform(post("/api/demandes-adhesion/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRMED"));

        demandeRepository.flush();
        DemandeAdhesion confirmed = demandeRepository.findById(demande.getId()).orElseThrow();
        assertThat(confirmed.getStatut()).isEqualTo(StatutDemande.EN_ATTENTE);
        assertThat(confirmed.getDateConfirmationEmail()).isNotNull();
        assertThat(confirmed.getEmailActifNormalise()).isEqualTo(email);
        assertThat(confirmed.getConditionsAccepteesAt()).isNotNull();
        assertThat(confirmed.getConditionsVersion()).isEqualTo("v1.0");

        mockMvc.perform(post("/api/demandes-adhesion/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ALREADY_CONFIRMED"));

        List<GeneratedEmail> receiptEmails = generatedEmailRepository.findAll().stream()
                .filter(item -> demande.getId().equals(item.getDemandeAdhesionId()))
                .filter(item -> item.getType() == EmailNotificationType.MEMBERSHIP_REQUEST_RECEIVED)
                .toList();
        assertThat(receiptEmails).singleElement().satisfies(history ->
                assertThat(history.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED));
        assertThat(demandeRepository.findById(demande.getId()).orElseThrow().getStatut())
                .isEqualTo(StatutDemande.EN_ATTENTE);
    }

    @Test
    void resendRotatesHashKeepsFailedEmailHistoryAndInvalidatesOldToken() throws Exception {
        String oldRawToken = "old-resend-token";
        String oldHash = tokenService.hash(oldRawToken);
        String email = "resend-" + UUID.randomUUID() + "@terra.test";
        DemandeAdhesion demande = demandeRepository.saveAndFlush(DemandeAdhesion.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(StatutDemande.A_CONFIRMER)
                .confirmationEmailTokenHash(oldHash)
                .confirmationEmailExpiresAt(LocalDateTime.now().minusHours(1))
                .dateDernierEnvoiConfirmation(LocalDateTime.now().minusMinutes(6))
                .conditionsAccepteesAt(LocalDateTime.now().minusDays(1))
                .conditionsVersion("v1.0")
                .build());
        long emailCountBefore = confirmationEmailCount(demande.getId());
        LocalDateTime before = LocalDateTime.now();

        mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                        .with(remoteAddress("192.0.2.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(genericResendMessage()));

        demandeRepository.flush();
        DemandeAdhesion updated = demandeRepository.findById(demande.getId()).orElseThrow();
        assertThat(updated.getConfirmationEmailTokenHash()).hasSize(64).isNotEqualTo(oldHash);
        assertThat(updated.getConfirmationEmailExpiresAt())
                .isBetween(before.plusHours(24), LocalDateTime.now().plusHours(24).plusSeconds(2));
        assertThat(updated.getDateDernierEnvoiConfirmation())
                .isBetween(before, LocalDateTime.now().plusSeconds(2));
        assertThat(updated.getStatut()).isEqualTo(StatutDemande.A_CONFIRMER);
        assertThat(updated.getConditionsVersion()).isEqualTo("v1.0");
        assertThat(confirmationEmailCount(demande.getId())).isEqualTo(emailCountBefore + 1);
        assertThat(generatedEmailRepository.findAll().stream()
                .filter(item -> demande.getId().equals(item.getDemandeAdhesionId()))
                .filter(item -> item.getType() == EmailNotificationType.MEMBERSHIP_EMAIL_CONFIRMATION)
                .reduce((first, second) -> second)
                .orElseThrow()
                .getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);

        mockMvc.perform(post("/api/demandes-adhesion/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + oldRawToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("INVALID"));

        String rotatedHash = updated.getConfirmationEmailTokenHash();
        mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                        .with(remoteAddress("192.0.2.21"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(genericResendMessage()));

        assertThat(demandeRepository.findById(demande.getId()).orElseThrow().getConfirmationEmailTokenHash())
                .isEqualTo(rotatedHash);
        assertThat(confirmationEmailCount(demande.getId())).isEqualTo(emailCountBefore + 1);
    }

    @Test
    void resendAlwaysReturnsSameMessageForUnknownMemberAndNonEligibleRequests() throws Exception {
        long emailCountBefore = generatedEmailRepository.count();
        String unknownEmail = "unknown-" + UUID.randomUUID() + "@terra.test";
        String memberEmail = membreRepository.saveAndFlush(testMember()).getEmail();
        String pendingEmail = saveRequestWithStatus(StatutDemande.EN_ATTENTE);
        String approvedEmail = saveRequestWithStatus(StatutDemande.APPROUVEE);
        String rejectedEmail = saveRequestWithStatus(StatutDemande.REFUSEE);
        List<String> emails = List.of(
                unknownEmail,
                memberEmail,
                pendingEmail,
                approvedEmail,
                rejectedEmail
        );

        for (int index = 0; index < emails.size(); index++) {
            mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                            .with(remoteAddress("192.0.2." + (30 + index)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + emails.get(index) + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(genericResendMessage()));
        }

        assertThat(generatedEmailRepository.count()).isEqualTo(emailCountBefore);
    }

    @Test
    void publicEndpointsReturn429ByEmailAndIpWithoutAffectingAdminEndpoints() throws Exception {
        String repeatedEmail = "limited-" + UUID.randomUUID() + "@terra.test";
        for (int index = 0; index < 3; index++) {
            mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                            .with(remoteAddress("198.51.100." + (10 + index)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + repeatedEmail + "\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                        .with(remoteAddress("198.51.100.20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + repeatedEmail + "\"}"))
                .andExpect(status().isTooManyRequests());

        String repeatedIp = "198.51.100.30";
        for (int index = 0; index < 10; index++) {
            mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                            .with(remoteAddress(repeatedIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ip-limit-" + index + "@terra.test\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                        .with(remoteAddress(repeatedIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ip-limit-final@terra.test\"}"))
                .andExpect(status().isTooManyRequests());

        String creationLimitedIp = "198.51.100.40";
        for (int index = 0; index < 10; index++) {
            mockMvc.perform(post("/api/demandes-adhesion/confirmation/renvoyer")
                            .with(remoteAddress(creationLimitedIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"creation-limit-" + index + "@terra.test\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/demandes-adhesion")
                        .with(remoteAddress(creationLimitedIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload("blocked-creation-" + UUID.randomUUID() + "@terra.test")))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/api/demandes-adhesion")
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    private String createPayload(String email) {
        return createPayload(email, true);
    }

    private String createPayload(String email, Boolean accepted) {
        String acceptanceField = accepted == null
                ? ""
                : ",\n                  \"accepteConditions\": " + accepted;
        return """
                {
                  "nom": "Dupont",
                  "prenom": "Marie",
                  "email": "%s",
                  "telephone": "0470000000",
                  "adresse": "Rue Test 1",
                  "ville": "Bruxelles",
                  "codePostal": "1000",
                  "pays": "Belgique",
                  "message": "Test confirmation"%s
                }
                """.formatted(email, acceptanceField);
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private String genericResendMessage() {
        return "Si une demande a confirmer existe pour cette adresse, un nouveau message de confirmation a ete envoye.";
    }

    private long confirmationEmailCount(Long demandeId) {
        return generatedEmailRepository.findAll().stream()
                .filter(item -> demandeId.equals(item.getDemandeAdhesionId()))
                .filter(item -> item.getType() == EmailNotificationType.MEMBERSHIP_EMAIL_CONFIRMATION)
                .count();
    }

    private String saveRequestWithStatus(StatutDemande status) {
        String email = status.name().toLowerCase() + "-" + UUID.randomUUID() + "@terra.test";
        demandeRepository.saveAndFlush(DemandeAdhesion.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .statut(status)
                .conditionsAccepteesAt(LocalDateTime.now().minusDays(1))
                .conditionsVersion("v1.0")
                .build());
        return email;
    }

    private Membre testMember() {
        return Membre.builder()
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
                .build();
    }
}
