package com.example.demo.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Categorie;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.TagAlimentaire;
import com.example.demo.model.enums.UniteProduit;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Profile({"dev", "test"})
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AdministrateurRepository administrateurRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final PointCollecteRepository pointCollecteRepository;
    private final CreneauCollecteRepository creneauCollecteRepository;
    private final CommandeRepository commandeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Administrateur admin = seedAdministrateur();

        Categorie legumes = seedCategorie(
                "Légumes",
                "Légumes frais proposés par les producteurs partenaires.",
                "Légumes bio",
                "Legumes bio"
        );
        Categorie fruits = seedCategorie(
                "Fruits de saison",
                "Fruits sélectionnés selon la saison."
        );
        Categorie epicerie = seedCategorie(
                "Épicerie locale",
                "Boissons et produits d'épicerie issus de producteurs locaux.",
                "Epicerie locale"
        );

        seedProduit(
                "Carottes longues jaunes de Belgique",
                "Ancienne variété belge à la chair aromatique et légèrement sucrée, adaptée aux salades, soupes, purées et plats mijotés.",
                decimal("2.74"),
                UniteProduit.KG,
                decimal("25.00"),
                legumes,
                EnumSet.of(TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/carottes-bio.jpg", "Belgique", "", "1 kg", false, true, true, true),
                "Carottes bio"
        );
        seedProduit(
                "Pommes de terre Annabelle - 5 kg",
                "Pommes de terre Annabelle à chair ferme, proposées en sac de 5 kg. Elles conviennent aux cuissons vapeur, salades, gratins et pommes sautées.",
                decimal("7.08"),
                UniteProduit.PIECE,
                decimal("20.00"),
                legumes,
                EnumSet.of(TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/pommes-de-terre.jpg", "", "L'Épi lacté", "sac de 5 kg", false, false, true, true),
                "Pommes de terre"
        );
        seedProduit(
                "Pommes Jonagold bio",
                "Pommes Jonagold belges à la chair croquante et juteuse, au goût doux, fruité et légèrement acidulé. Idéales à croquer, en compote ou en pâtisserie.",
                decimal("3.29"),
                UniteProduit.KG,
                decimal("30.00"),
                fruits,
                EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/pomme-jonagold.webp", "Belgique", "", "1 kg", true, true, true, true),
                "Pommes Jonagold"
        );
        seedProduit(
                "Jus de pomme D'Upigny - 3 L",
                "Jus de pomme pasteurisé belge, sans additif ni sucre ajouté. Format familial de 3 litres avec robinet, à servir frais et à conserver au réfrigérateur après ouverture.",
                decimal("8.48"),
                UniteProduit.PIECE,
                decimal("15.00"),
                epicerie,
                EnumSet.of(TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/jus-de-pomme-artisanal.jpg", "Upigny, Belgique", "Ferme d'Assonleville", "poche de 3 L", false, true, true, true),
                "Jus de pomme artisanal"
        );
        seedProduit(
                "Œufs de ferme - boîte de 6",
                "Boîte de six œufs de ferme d'origine belge, à conserver au frais après achat.",
                decimal("2.26"),
                UniteProduit.BARQUETTE,
                decimal("25.00"),
                epicerie,
                EnumSet.of(TagAlimentaire.LOCAL, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/oeufs.jpg", "Belgique", "La Ferme Belge", "boîte de 6", false, true, false, true),
                "Œufs fermiers x6",
                "Oeufs fermiers"
        );

        PointCollecte pointCollecte = seedPointCollecte(
                "Dépôt Bruxelles Centre",
                "Rue du Marché 12, 1000 Bruxelles",
                "Depot Bruxelles Centre"
        );

        LocalDateTime now = LocalDateTime.now();
        seedCreneau(
                pointCollecte,
                now.toLocalDate().plusDays(2),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                now
        );
        seedCreneau(
                pointCollecte,
                now.toLocalDate().plusDays(3),
                LocalTime.of(18, 30),
                LocalTime.of(20, 30),
                now
        );

        log.info(
                "Données de référence prêtes : admin={}, catégories={}, produits={}, points={}, créneaux={}, commandes={}",
                admin.getEmail(),
                categorieRepository.count(),
                produitRepository.count(),
                pointCollecteRepository.count(),
                creneauCollecteRepository.count(),
                commandeRepository.count()
        );
    }

    private Administrateur seedAdministrateur() {
        return administrateurRepository.findAll().stream()
                .filter(admin -> equalsIgnoreCase(admin.getEmail(), "admin.demo@terrasana.test"))
                .findFirst()
                .orElseGet(() -> administrateurRepository.save(
                        Administrateur.builder()
                                .nom("Demo")
                                .prenom("Admin")
                                .email("admin.demo@terrasana.test")
                                .motDePasse(passwordEncoder.encode("demo-password"))
                                .actif(true)
                                .build()
                ));
    }

    private Categorie seedCategorie(String nom, String description, String... anciensNoms) {
        Categorie categorie = categorieRepository.findAll().stream()
                .filter(item -> matchesName(item.getNom(), nom, anciensNoms))
                .findFirst()
                .orElse(null);

        if (categorie == null) {
            return categorieRepository.save(Categorie.builder()
                    .nom(nom)
                    .description(description)
                    .actif(true)
                    .build());
        }

        if (!equalsIgnoreCase(categorie.getNom(), nom)) {
            categorie.setNom(nom);
            categorie.setDescription(description);
            return categorieRepository.save(categorie);
        }

        return categorie;
    }

    private Produit seedProduit(
            String nom,
            String description,
            BigDecimal prixUnitaire,
            UniteProduit unite,
            BigDecimal stockActuel,
            Categorie categorie,
            Set<TagAlimentaire> tagsAlimentaires,
            String images,
            String... anciensNoms
    ) {
        Produit produit = produitRepository.findAllWithTags().stream()
                .filter(item -> matchesName(item.getNom(), nom, anciensNoms))
                .findFirst()
                .orElse(null);

        if (produit == null) {
            return produitRepository.save(Produit.builder()
                    .nom(nom)
                    .description(description)
                    .prixUnitaire(prixUnitaire)
                    .unite(unite)
                    .tauxTva(decimal("6.00"))
                    .stockActuel(stockActuel)
                    .images(images)
                    .actif(true)
                    .tagsAlimentaires(tagsAlimentaires)
                    .categorie(categorie)
                    .build());
        }

        // Migrate only legacy demo records once. Later admin edits remain persistent.
        if (!equalsIgnoreCase(produit.getNom(), nom)) {
            produit.setNom(nom);
            produit.setDescription(description);
            produit.setPrixUnitaire(prixUnitaire);
            produit.setUnite(unite);
            produit.setTauxTva(decimal("6.00"));
            produit.setImages(images);
            produit.setCategorie(categorie);
            produit.getTagsAlimentaires().clear();
            produit.getTagsAlimentaires().addAll(tagsAlimentaires);
            return produitRepository.save(produit);
        }

        return produit;
    }

    private PointCollecte seedPointCollecte(String nom, String adresse, String... anciensNoms) {
        return pointCollecteRepository.findAll().stream()
                .filter(point -> matchesName(point.getNom(), nom, anciensNoms))
                .findFirst()
                .orElseGet(() -> pointCollecteRepository.save(
                        PointCollecte.builder()
                                .nom(nom)
                                .adresse(adresse)
                                .actif(true)
                                .build()
                ));
    }

    private CreneauCollecte seedCreneau(
            PointCollecte pointCollecte,
            LocalDate date,
            LocalTime heureDebut,
            LocalTime heureFin,
            LocalDateTime now
    ) {
        List<CreneauCollecte> creneaux = creneauCollecteRepository.findByPointCollecte_Id(pointCollecte.getId());

        return creneaux.stream()
                .filter(creneau -> Boolean.TRUE.equals(creneau.getActif()))
                .filter(creneau -> isBeyondMinimumOrderLeadTime(creneau, now))
                .filter(creneau -> Objects.equals(creneau.getHeureDebut(), heureDebut))
                .filter(creneau -> Objects.equals(creneau.getHeureFin(), heureFin))
                .min(Comparator.comparing(CreneauCollecte::getCreneau))
                .orElseGet(() -> createFirstAvailableCreneau(pointCollecte, creneaux, date, heureDebut, heureFin));
    }

    static boolean isBeyondMinimumOrderLeadTime(CreneauCollecte creneau, LocalDateTime now) {
        if (creneau == null || creneau.getCreneau() == null || creneau.getHeureDebut() == null || now == null) {
            return false;
        }
        return LocalDateTime.of(creneau.getCreneau(), creneau.getHeureDebut())
                .isAfter(now.plusHours(24));
    }

    private CreneauCollecte createFirstAvailableCreneau(
            PointCollecte pointCollecte,
            List<CreneauCollecte> existingCreneaux,
            LocalDate firstDate,
            LocalTime heureDebut,
            LocalTime heureFin
    ) {
        LocalDate candidateDate = firstDate;
        for (int offset = 0; offset < 30; offset++) {
            LocalDate dateToCheck = candidateDate;
            boolean alreadyExists = existingCreneaux.stream()
                .filter(creneau -> Objects.equals(creneau.getCreneau(), dateToCheck))
                .filter(creneau -> Objects.equals(creneau.getHeureDebut(), heureDebut))
                .filter(creneau -> Objects.equals(creneau.getHeureFin(), heureFin))
                .findFirst()
                .isPresent();

            if (!alreadyExists) {
                return creneauCollecteRepository.save(
                        CreneauCollecte.builder()
                                .pointCollecte(pointCollecte)
                                .creneau(dateToCheck)
                                .heureDebut(heureDebut)
                                .heureFin(heureFin)
                                .actif(true)
                                .build()
                );
            }
            candidateDate = candidateDate.plusDays(1);
        }

        throw new IllegalStateException("Impossible de créer un créneau de démonstration sans doublon.");
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean matchesName(String actualName, String expectedName, String... legacyNames) {
        if (equalsIgnoreCase(actualName, expectedName)) {
            return true;
        }
        return List.of(legacyNames).stream()
                .anyMatch(legacyName -> equalsIgnoreCase(actualName, legacyName));
    }

    private String buildImagesMeta(
            String image,
            String origine,
            String producteur,
            String conditionnement,
            boolean bio,
            boolean local,
            boolean vegan,
            boolean vegetarien
    ) {
        return "{"
                + "\"image\":\"" + image + "\","
                + "\"photos\":[],"
                + "\"badges\":{"
                + "\"bio\":" + bio + ","
                + "\"local\":" + local + ","
                + "\"vegan\":" + vegan + ","
                + "\"vegetarien\":" + vegetarien
                + "},"
                + "\"origine\":\"" + origine + "\","
                + "\"producteur\":\"" + producteur + "\","
                + "\"conditionnement\":\"" + conditionnement + "\""
                + "}";
    }
}
