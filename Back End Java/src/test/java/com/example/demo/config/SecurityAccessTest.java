package com.example.demo.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.Membre;
import com.example.demo.model.enums.StatutCommande;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {
        "app.order-preparation.email.enabled=true",
        "app.order-preparation.email.recipient=preparation@terrasana.test"
})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicLoginRejectsMissingCsrfAndAcceptsTheSessionHeader() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        var bootstrap = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var token = objectMapper.readTree(bootstrap.getResponse().getContentAsString());
        var session = (MockHttpSession) bootstrap.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(post("/api/auth/login").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticatedMemberCannotReachH2Console() throws Exception {
        mockMvc.perform(get("/h2-console/").with(user("member").roles("MEMBRE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiRejectsUnconfiguredOrigins() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").header(HttpHeaders.ORIGIN, "https://untrusted.example.test"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private CreneauCollecteRepository creneauCollecteRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Test
    void adminEndpointWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Authentification requise.\"}"));
    }

    @Test
    void adminEndpointWithMemberRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"message\":\"Acces refuse.\"}"));
    }

    @Test
    void memberEndpointWithAdminRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/commandes/moi")
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"message\":\"Acces refuse.\"}"));
    }

    @Test
    void adminEndpointWithAdminRoleIsAllowed() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void memberCatalogEndpointsWithoutAuthenticationReturn401() throws Exception {
        for (String endpoint : new String[]{
                "/api/categories/actives",
                "/api/produits/actives",
                "/api/points-collecte/actifs"
        }) {
            mockMvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"message\":\"Authentification requise.\"}"));
        }
    }

    @Test
    void memberCatalogEndpointsWithMemberRoleAreAllowed() throws Exception {
        for (String endpoint : new String[]{
                "/api/categories/actives",
                "/api/produits/actives",
                "/api/points-collecte/actifs"
        }) {
            mockMvc.perform(get(endpoint)
                            .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void administrativeCatalogEndpointsRequireAdminRole() throws Exception {
        for (String endpoint : new String[]{
                "/api/categories",
                "/api/produits",
                "/api/points-collecte"
        }) {
            mockMvc.perform(get(endpoint)
                            .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(endpoint)
                            .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void publicMembershipAndPasswordResetEndpointsRemainPublic() throws Exception {
        mockMvc.perform(post("/api/demandes-adhesion").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/auth/password-reset/token-inexistant"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"valid\":false}"));
    }

    @Test
    void preparationIncidentEndpointsRequireAdminRoleAndValidateReason() throws Exception {
        String endpoint = "/api/commandes/999999/incident-preparation";

        mockMvc.perform(put(endpoint).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Produit manquant\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put(endpoint).with(csrf())
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Produit manquant\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(endpoint).with(csrf())
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(endpoint).with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(endpoint).with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"Produit manquant\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void preparationGroupFinalizationRequiresAdminRole() throws Exception {
        String endpoint = "/api/admin/preparation/groupes/999999/finaliser";

        mockMvc.perform(post(endpoint).with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(endpoint).with(csrf())
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(endpoint).with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void preparationPdfRequiresAdminRoleAndRejectsUnknownSlot() throws Exception {
        String endpoint = "/api/admin/preparation/groupes/999999/pdf";

        mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(endpoint)
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(endpoint)
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void preparationEmailRequiresAdminRoleAndRejectsUnknownSlot() throws Exception {
        String endpoint = "/api/admin/preparation/groupes/999999/email";

        mockMvc.perform(post(endpoint).with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(endpoint).with(csrf())
                        .with(user("membre.demo@terrasana.test").roles("MEMBRE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(endpoint).with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void failedManualPreparationEmailKeepsOrderStateAndPdfAvailable() throws Exception {
        Commande commande = saveReadyOrder();
        Long creneauId = commande.getCreneauCollecte().getId();
        var initialStatus = commande.getStatut();

        mockMvc.perform(post("/api/admin/preparation/groupes/{creneauId}/email", creneauId).with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutEnvoi").value("FAILED"))
                .andExpect(jsonPath("$.envoiConfigure").value(true));

        assertThat(commandeRepository.findById(commande.getId()).orElseThrow().getStatut())
                .isEqualTo(initialStatus);

        byte[] pdf = mockMvc.perform(get("/api/admin/preparation/groupes/{creneauId}/pdf", creneauId)
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(pdf).hasSizeGreaterThan(4);
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
    }

    @Test
    void adminCanDownloadPreparationPdfWithExpectedHeadersAndContent() throws Exception {
        Long creneauId = creneauCollecteRepository.findAll().get(0).getId();

        byte[] content = mockMvc.perform(get("/api/admin/preparation/groupes/{creneauId}/pdf", creneauId)
                        .with(user("admin.demo@terrasana.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString(".pdf")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(content).hasSizeGreaterThan(4);
        assertThat(new String(content, 0, 4, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
    }

    @Test
    void withdrawalEndpointsRequireAdminRole() throws Exception {
        for (String endpoint : new String[]{
                "/api/admin/retraits/rechercher",
                "/api/admin/retraits/confirmer"
        }) {
            mockMvc.perform(post(endpoint).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codeRetrait\":\"UNKNOWN\"}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post(endpoint).with(csrf())
                            .with(user("membre.demo@terrasana.test").roles("MEMBRE"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codeRetrait\":\"UNKNOWN\"}"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(endpoint).with(csrf())
                            .with(user("admin.demo@terrasana.test").roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codeRetrait\":\"UNKNOWN\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void withdrawalSearchReturnsOnlyRequiredData() throws Exception {
        Commande commande = saveReadyOrder();
        String code = commande.getCodeRetrait().toLowerCase();

        mockMvc.perform(post("/api/admin/retraits/rechercher").with(csrf())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codeRetrait\":\"  " + code + "  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandeId").value(commande.getId()))
                .andExpect(jsonPath("$.numeroCommande").value(commande.getNumero()))
                .andExpect(jsonPath("$.codeRetrait").value(commande.getCodeRetrait()))
                .andExpect(jsonPath("$.membreNom").exists())
                .andExpect(jsonPath("$.membrePrenom").exists())
                .andExpect(jsonPath("$.pointCollecte").exists())
                .andExpect(jsonPath("$.statut").exists())
                .andExpect(jsonPath("$.produits").doesNotExist())
                .andExpect(jsonPath("$.membreEmail").doesNotExist())
                .andExpect(jsonPath("$.montantHT").doesNotExist())
                .andExpect(jsonPath("$.datePaiement").value(org.hamcrest.Matchers.nullValue()));
    }

    private Commande saveReadyOrder() {
        CreneauCollecte creneau = creneauCollecteRepository.findAll().get(0);
        Membre membre = membreRepository.saveAndFlush(Membre.builder()
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
        return commandeRepository.saveAndFlush(Commande.builder()
                .numero("TEST-SECURITY-" + UUID.randomUUID())
                .codeRetrait(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .statut(StatutCommande.READY)
                .montantHT(BigDecimal.ZERO)
                .montantTVA(BigDecimal.ZERO)
                .montantTTC(BigDecimal.ZERO)
                .paye(false)
                .dateCommande(LocalDateTime.now())
                .dateRetraitPrevu(LocalDateTime.of(creneau.getCreneau(), creneau.getHeureDebut()))
                .membre(membre)
                .creneauCollecte(creneau)
                .build());
    }
}
