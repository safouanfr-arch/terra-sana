package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.example.demo.dao.PromotionRepository;
import com.example.demo.model.Produit;
import com.example.demo.model.Promotion;
import com.example.demo.model.enums.TypeReduction;
import com.example.demo.model.enums.UniteProduit;

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
    private PromotionRepository promotionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .contains("Légumes", "Fruits de saison", "Épicerie locale");
        assertThat(produitRepository.findAll())
                .extracting("nom")
                .containsExactlyInAnyOrder(
                        "Carottes longues jaunes de Belgique",
                        "Pommes de terre Annabelle - 5 kg",
                        "Pommes Jonagold bio",
                        "Jus de pomme D'Upigny - 3 L",
                        "Œufs de ferme - boîte de 6"
                );

        Produit carottes = produitRepository.findAll().stream()
                .filter(produit -> "Carottes longues jaunes de Belgique".equals(produit.getNom()))
                .findFirst()
                .orElseThrow();
        String customDescription = "Description personnalisée conservée.";
        carottes.setDescription(customDescription);
        produitRepository.saveAndFlush(carottes);

        demoDataSeeder.run();

        assertThat(produitRepository.findById(carottes.getId()).orElseThrow().getDescription())
                .isEqualTo(customDescription);

        Produit jus = produitRepository.findAll().stream()
                .filter(produit -> "Jus de pomme D'Upigny - 3 L".equals(produit.getNom()))
                .findFirst()
                .orElseThrow();
        assertThat(jus.getUnite()).isEqualTo(UniteProduit.PIECE);
        assertThat(jus.getPrixUnitaire()).isEqualByComparingTo("8.48");
        assertThat(jus.getImages()).contains("\"conditionnement\":\"poche de 3 L\"");

        for (String nom : List.of(
                "Carottes longues jaunes de Belgique",
                "Pommes de terre Annabelle - 5 kg",
                "Pommes Jonagold bio",
                "Jus de pomme D'Upigny - 3 L",
                "Œufs de ferme - boîte de 6"
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

    @Test
    void allPersistentEnumColumnsUseVarcharInsteadOfClosedH2Enums() {
        for (EnumColumn column : List.of(
                new EnumColumn("TCOMMANDE", "STATUT"),
                new EnumColumn("TMOUVEMENT_STOCK", "TYPE_MOUVEMENT"),
                new EnumColumn("TPRODUIT", "UNITE"),
                new EnumColumn("TPRODUIT_TAGS", "TAG"),
                new EnumColumn("TPROMOTION", "TYPE_REDUCTION"),
                new EnumColumn("TDEMANDE_ADHESION", "STATUT"),
                new EnumColumn("TEMAIL_GENERE", "TYPE_EMAIL"),
                new EnumColumn("TEMAIL_GENERE", "STATUT_ENVOI"),
                new EnumColumn("TEMAIL_TEMPLATE", "TYPE_TEMPLATE")
        )) {
            String dataType = jdbcTemplate.queryForObject(
                    """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? AND COLUMN_NAME = ?
                    """,
                    String.class,
                    column.tableName(),
                    column.columnName()
            );
            assertThat(dataType)
                    .as("SQL type of %s.%s", column.tableName(), column.columnName())
                    .isEqualTo("CHARACTER VARYING");
        }
    }

    @Test
    @Transactional
    void enumValueCanBeInsertedAndReadAsAString() {
        Promotion promotion = promotionRepository.saveAndFlush(Promotion.builder()
                .nom("Promotion test VARCHAR")
                .typeReduction(TypeReduction.MONTANT_FIXE)
                .valeurReduction(new BigDecimal("2.50"))
                .dateDebut(LocalDateTime.of(2026, 10, 1, 0, 0))
                .dateFin(LocalDateTime.of(2026, 10, 31, 23, 59))
                .actif(true)
                .build());

        String storedValue = jdbcTemplate.queryForObject(
                "SELECT TYPE_REDUCTION FROM TPROMOTION WHERE IDPROMOTION = ?",
                String.class,
                promotion.getId()
        );

        assertThat(storedValue).isEqualTo("MONTANT_FIXE");
        assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().getTypeReduction())
                .isEqualTo(TypeReduction.MONTANT_FIXE);
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

    private record EnumColumn(String tableName, String columnName) {
    }
}
