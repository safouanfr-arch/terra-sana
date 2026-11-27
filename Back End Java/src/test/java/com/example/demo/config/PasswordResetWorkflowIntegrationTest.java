package com.example.demo.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.GeneratedEmailRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.model.GeneratedEmail;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.EmailNotificationType;
import com.example.demo.model.enums.EmailSendStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetWorkflowIntegrationTest {

    private static final String PUBLIC_MESSAGE =
            "Si un compte correspond à cette adresse, un email de réinitialisation a été envoyé.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private GeneratedEmailRepository generatedEmailRepository;

    @Test
    void knownUnknownAndInactiveEmailsHaveTheSamePublicResponseWithoutAnySecret() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Membre active = membreRepository.save(member("active-" + suffix + "@terra.test", true));
        Membre inactive = membreRepository.save(member("inactive-" + suffix + "@terra.test", false));
        String unknownEmail = "unknown-" + suffix + "@terra.test";

        JsonNode knownResponse = requestReset(active.getEmail());
        long emailsAfterKnownRequest = passwordSetupEmailsFor(active.getId()).size();
        JsonNode unknownResponse = requestReset(unknownEmail);
        JsonNode inactiveResponse = requestReset(inactive.getEmail());

        assertGenericResponse(knownResponse);
        assertThat(unknownResponse).isEqualTo(knownResponse);
        assertThat(inactiveResponse).isEqualTo(knownResponse);
        assertThat(emailsAfterKnownRequest).isEqualTo(1);
        assertThat(passwordSetupEmailsFor(active.getId())).singleElement().satisfies(history -> {
            assertThat(history.getStatutEnvoi()).isEqualTo(EmailSendStatus.FAILED);
            assertThat(history.getContenuHtml())
                    .doesNotContain("creation-mot-de-passe/")
                    .contains("non conserve");
        });
        assertThat(passwordSetupEmailsFor(inactive.getId())).isEmpty();
        assertThat(active.getTokenMotDePasse()).hasSize(64);
    }

    private JsonNode requestReset(String email) throws Exception {
        String content = mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("email", email))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private void assertGenericResponse(JsonNode response) {
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.path("message").asText()).isEqualTo(PUBLIC_MESSAGE);
        assertThat(response.has("token")).isFalse();
        assertThat(response.has("resetLink")).isFalse();
        assertThat(response.has("email")).isFalse();
        assertThat(response.has("expiresAt")).isFalse();
    }

    private List<GeneratedEmail> passwordSetupEmailsFor(Long memberId) {
        return generatedEmailRepository.findAll().stream()
                .filter(email -> email.getType() == EmailNotificationType.PASSWORD_SETUP)
                .filter(email -> memberId.equals(email.getMembreId()))
                .toList();
    }

    private Membre member(String email, boolean active) {
        return Membre.builder()
                .nom("Dupont")
                .prenom("Marie")
                .email(email)
                .telephone("0470000000")
                .adresse("Rue Test 1")
                .ville("Bruxelles")
                .codePostal("1000")
                .pays("Belgique")
                .motDePasse("mot-de-passe-inutilise")
                .actif(active)
                .build();
    }
}
