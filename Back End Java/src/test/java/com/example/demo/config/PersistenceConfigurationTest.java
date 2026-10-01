package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.model.Produit;

@SpringBootTest
@ActiveProfiles("test")
class PersistenceConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @Autowired
    private AdministrateurRepository administrateurRepository;

    @Autowired
    private MembreRepository membreRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private PointCollecteRepository pointCollecteRepository;

    @Autowired
    private CreneauCollecteRepository creneauCollecteRepository;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Test
    void testProfileUsesInMemoryH2Datasource() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:");
    }

    @Test
    void demoDataSeederDoesNotDuplicateDataWhenRunSeveralTimes() {
        Counts before = currentCounts();

        demoDataSeeder.run();
        demoDataSeeder.run();

        assertThat(currentCounts()).isEqualTo(before);
    }

    @Test
    @Transactional
    void demoCatalogUsesCoherentLocalProductsWithoutOverwritingExistingData() {
        assertThat(categorieRepository.findAll())
                .extracting("nom")
                .contains("Légumes bio", "Fruits de saison", "Épicerie locale");
        assertThat(produitRepository.findAll())
                .extracting("nom")
                .containsExactlyInAnyOrder(
                        "Carottes bio",
                        "Pommes de terre",
                        "Pommes Jonagold",
                        "Jus de pomme artisanal",
                        "Œufs fermiers x6"
                );

        Produit carottes = produitRepository.findAll().stream()
                .filter(produit -> "Carottes bio".equals(produit.getNom()))
                .findFirst()
                .orElseThrow();
        String customDescription = "Description personnalisée conservée.";
        carottes.setDescription(customDescription);
        produitRepository.saveAndFlush(carottes);

        demoDataSeeder.run();

        assertThat(produitRepository.findById(carottes.getId()).orElseThrow().getDescription())
                .isEqualTo(customDescription);
        for (String nom : List.of(
                "Carottes bio",
                "Pommes de terre",
                "Pommes Jonagold",
                "Jus de pomme artisanal",
                "Œufs fermiers x6"
        )) {
            Produit produit = produitRepository.findAll().stream()
                    .filter(item -> nom.equals(item.getNom()))
                    .findFirst()
                    .orElseThrow();
            assertThat(produit.getImages()).contains("/product-images/");
        }
    }

    @Test
    void springMailSenderBeanIsAvailable() {
        assertThat(mailSenderProvider.getIfAvailable()).isNotNull();
    }

    private Counts currentCounts() {
        return new Counts(
                administrateurRepository.count(),
                membreRepository.count(),
                categorieRepository.count(),
                produitRepository.count(),
                pointCollecteRepository.count(),
                creneauCollecteRepository.count(),
                commandeRepository.count()
        );
    }

    private record Counts(
            long administrateurs,
            long membres,
            long categories,
            long produits,
            long pointsCollecte,
            long creneaux,
            long commandes
    ) {
    }
}
