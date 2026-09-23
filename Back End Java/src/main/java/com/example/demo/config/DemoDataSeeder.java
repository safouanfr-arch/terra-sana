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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.dao.AdministrateurRepository;
import com.example.demo.dao.CategorieRepository;
import com.example.demo.dao.CommandeRepository;
import com.example.demo.dao.CreneauCollecteRepository;
import com.example.demo.dao.MembreRepository;
import com.example.demo.dao.PointCollecteRepository;
import com.example.demo.dao.ProduitRepository;
import com.example.demo.dto.CommandeCreateItemDto;
import com.example.demo.dto.CommandeCreateRequestDto;
import com.example.demo.dto.CommandeResponseDto;
import com.example.demo.dto.CommandeStatutUpdateDto;
import com.example.demo.model.Administrateur;
import com.example.demo.model.Categorie;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.Membre;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.TagAlimentaire;
import com.example.demo.model.enums.UniteProduit;
import com.example.demo.service.CommandeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AdministrateurRepository administrateurRepository;
    private final MembreRepository membreRepository;
    private final CategorieRepository categorieRepository;
    private final ProduitRepository produitRepository;
    private final PointCollecteRepository pointCollecteRepository;
    private final CreneauCollecteRepository creneauCollecteRepository;
    private final CommandeRepository commandeRepository;
    private final CommandeService commandeService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Administrateur admin = seedAdministrateur();
        Membre membre = seedMembre();

        Categorie legumes = seedCategorie("Legumes bio", "Selection de legumes frais pour la demo.");
        Categorie fruits = seedCategorie("Fruits de saison", "Fruits de saison visibles dans le catalogue.");
        Categorie epicerie = seedCategorie("Epicerie locale", "Produits secs et boissons pour la demo.");

        Produit carottes = seedProduit(
                "Carottes bio",
                "Carottes locales de saison.",
                decimal("3.40"),
                UniteProduit.KG,
                decimal("80.00"),
                legumes,
                EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/carottes-bio.jpg", "Belgique", "Ferme du Parc", true, true, true, true)
        );
        Produit pommesTerre = seedProduit(
                "Pommes de terre",
                "Pommes de terre polyvalentes pour cuisson vapeur et four.",
                decimal("2.80"),
                UniteProduit.KG,
                decimal("120.00"),
                legumes,
                EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/pommes-de-terre.jpg", "Belgique", "Maraicher des Flandres", true, true, true, true)
        );
        Produit pommes = seedProduit(
                "Pommes Jonagold",
                "Pommes croquantes issues d'un verger belge.",
                decimal("4.20"),
                UniteProduit.KG,
                decimal("60.00"),
                fruits,
                EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/pomme-jonagold.webp", "Belgique", "Verger Saint Martin", true, true, true, true)
        );
        Produit jusPomme = seedProduit(
                "Jus de pomme artisanal",
                "Jus de pomme local non filtre.",
                decimal("5.50"),
                UniteProduit.LITRE,
                decimal("24.00"),
                epicerie,
                EnumSet.of(TagAlimentaire.BIO, TagAlimentaire.LOCAL, TagAlimentaire.VEGAN, TagAlimentaire.VEGETARIEN),
                buildImagesMeta("/product-images/jus-de-pomme-artisanal.jpg", "Belgique", "Atelier du Verger", true, true, true, true)
        );
        Produit oeufs = seedProduit(
                "Oeufs fermiers",
                "Barquette d'oeufs fermiers.",
                decimal("4.90"),
                UniteProduit.BARQUETTE,
                decimal("18.00"),
                epicerie,
                EnumSet.of(TagAlimentaire.LOCAL),
                buildImagesMeta("/product-images/oeufs.jpg", "Belgique", "Ferme des Pres", false, true, false, true)
        );

        PointCollecte pointCollecte = seedPointCollecte(
                "Depot Bruxelles Centre",
                "Rue du Marche 12, 1000 Bruxelles"
        );

        CreneauCollecte creneau1 = seedCreneau(pointCollecte, LocalDate.now().plusDays(1), LocalTime.of(18, 0), LocalTime.of(20, 0));
        CreneauCollecte creneau2 = seedCreneau(pointCollecte, LocalDate.now().plusDays(2), LocalTime.of(18, 30), LocalTime.of(20, 30));

        seedCommandesDemoIfMissing(membre, creneau1, creneau2, carottes, pommesTerre, pommes, jusPomme, oeufs);

        log.info(
                "Demo seed pret: admin={}, membre={}, categories={}, produits={}, points={}, creneaux={}, commandes={}",
                admin.getEmail(),
                membre.getEmail(),
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

    private Membre seedMembre() {
        return membreRepository.findByEmailIgnoreCase("membre.demo@terrasana.test")
                .orElseGet(() -> membreRepository.save(
                        Membre.builder()
                                .nom("Demo")
                                .prenom("Membre")
                                .email("membre.demo@terrasana.test")
                                .telephone("0470000000")
                                .adresse("Rue des Tests 10")
                                .ville("Bruxelles")
                                .codePostal("1000")
                                .pays("Belgique")
                                .motDePasse(passwordEncoder.encode("demo-password"))
                                .actif(true)
                                .build()
                ));
    }

    private Categorie seedCategorie(String nom, String description) {
        return categorieRepository.findAll().stream()
                .filter(categorie -> equalsIgnoreCase(categorie.getNom(), nom))
                .findFirst()
                .orElseGet(() -> categorieRepository.save(
                        Categorie.builder()
                                .nom(nom)
                                .description(description)
                                .actif(true)
                                .build()
                ));
    }

    private Produit seedProduit(
            String nom,
            String description,
            BigDecimal prixUnitaire,
            UniteProduit unite,
            BigDecimal stockActuel,
            Categorie categorie,
            Set<TagAlimentaire> tagsAlimentaires,
            String images
    ) {
        return produitRepository.findAll().stream()
                .filter(produit -> equalsIgnoreCase(produit.getNom(), nom))
                .filter(produit -> produit.getUnite() == unite)
                .filter(produit -> produit.getCategorie() != null)
                .filter(produit -> Objects.equals(produit.getCategorie().getId(), categorie.getId()))
                .findFirst()
                .orElseGet(() -> produitRepository.save(
                        Produit.builder()
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
                                .build()
                ));
    }

    private PointCollecte seedPointCollecte(String nom, String adresse) {
        return pointCollecteRepository.findAll().stream()
                .filter(point -> equalsIgnoreCase(point.getNom(), nom))
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
            LocalTime heureFin
    ) {
        List<CreneauCollecte> creneaux = creneauCollecteRepository.findByPointCollecte_Id(pointCollecte.getId());

        return creneaux.stream()
                .filter(creneau -> Boolean.TRUE.equals(creneau.getActif()))
                .filter(creneau -> LocalDateTime.of(creneau.getCreneau(), creneau.getHeureDebut()).isAfter(LocalDateTime.now()))
                .filter(creneau -> Objects.equals(creneau.getHeureDebut(), heureDebut))
                .filter(creneau -> Objects.equals(creneau.getHeureFin(), heureFin))
                .min(Comparator.comparing(CreneauCollecte::getCreneau))
                .orElseGet(() -> createFirstAvailableCreneau(pointCollecte, creneaux, date, heureDebut, heureFin));
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

        throw new IllegalStateException("Impossible de creer un creneau de demonstration sans doublon.");
    }

    private void seedCommandesDemoIfMissing(
            Membre membre,
            CreneauCollecte creneau1,
            CreneauCollecte creneau2,
            Produit carottes,
            Produit pommesTerre,
            Produit pommes,
            Produit jusPomme,
            Produit oeufs
    ) {
        if (commandeRepository.count() > 0) {
            return;
        }

        commandeService.create(CommandeCreateRequestDto.builder()
                .membreId(membre.getId())
                .creneauCollecteId(creneau1.getId())
                .commentaire("Commande de demonstration a annuler ou preparer.")
                .produits(List.of(
                        item(carottes.getId(), "2.00"),
                        item(jusPomme.getId(), "1.00")
                ))
                .build());

        CommandeResponseDto commandeReady = commandeService.create(CommandeCreateRequestDto.builder()
                .membreId(membre.getId())
                .creneauCollecteId(creneau2.getId())
                .commentaire("Commande de demonstration deja preparee.")
                .produits(List.of(
                        item(pommesTerre.getId(), "3.00"),
                        item(pommes.getId(), "1.50"),
                        item(oeufs.getId(), "1.00")
                ))
                .build());

        commandeService.updateStatus(
                commandeReady.getId(),
                CommandeStatutUpdateDto.builder().statut(StatutCommande.READY).build()
        );
    }

    private CommandeCreateItemDto item(Long produitId, String quantite) {
        return CommandeCreateItemDto.builder()
                .produitId(produitId)
                .quantite(decimal(quantite))
                .build();
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String buildImagesMeta(
            String image,
            String origine,
            String producteur,
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
                + "\"producteur\":\"" + producteur + "\""
                + "}";
    }
}
