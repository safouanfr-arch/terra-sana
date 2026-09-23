package com.example.demo.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(post("/api/demandes-adhesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/auth/password-reset/token-inexistant"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"valid\":false}"));
    }
}
