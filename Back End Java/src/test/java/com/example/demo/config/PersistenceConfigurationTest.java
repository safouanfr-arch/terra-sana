package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dao.ProduitRepository;

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
