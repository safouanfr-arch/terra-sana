package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.MouvementStockRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.model.Produit;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProduitStockProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private MouvementStockRepository mouvementStockRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void genericProductUpdateIgnoresInjectedStockWithoutCreatingMovement() throws Exception {
        Produit produit = produitRepository.findAll().get(0);
        BigDecimal stockBefore = produit.getStockActuel();
        long movementCountBefore = mouvementStockRepository.count();
        String updatedName = produit.getNom() + " catalogue";

        Map<String, Object> payload = Map.of(
                "nom", updatedName,
                "description", produit.getDescription(),
                "prixUnitaire", produit.getPrixUnitaire().add(new BigDecimal("0.10")),
                "unite", produit.getUnite().name(),
                "tauxTva", produit.getTauxTva(),
                "stockActuel", new BigDecimal("-999.00"),
                "images", produit.getImages(),
                "actif", produit.getActif(),
                "tagsAlimentaires", List.of(),
                "categorieId", produit.getCategorie().getId()
        );

        mockMvc.perform(put("/api/produits/{id}", produit.getId())
                        .with(user("admin.demo@terrasana.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value(updatedName))
                .andExpect(jsonPath("$.stockActuel").value(stockBefore.doubleValue()));

        entityManager.flush();
        entityManager.clear();
        Produit reloaded = produitRepository.findById(produit.getId()).orElseThrow();
        assertThat(reloaded.getStockActuel()).isEqualByComparingTo(stockBefore);
        assertThat(mouvementStockRepository.count()).isEqualTo(movementCountBefore);
    }
}
