package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.Categorie;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Membre;
import com.example.demo.model.PointCollecte;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.UniteProduit;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

class PreparationPdfServiceTest {

    private PreparationPdfService service;
    private CreneauCollecte creneau;
    private Produit oeufs;
    private Produit lait;

    @BeforeEach
    void setUp() {
        service = new PreparationPdfService();
        ReflectionTestUtils.setField(
                service,
                "clock",
                Clock.fixed(Instant.parse("2026-10-09T08:15:00Z"), ZoneId.of("Europe/Brussels"))
        );

        PointCollecte point = PointCollecte.builder()
                .id(4L)
                .nom("Dépôt Bruxelles Centre")
                .adresse("Rue de l'Été 10, Bruxelles")
                .build();
        creneau = CreneauCollecte.builder()
                .id(7L)
                .creneau(LocalDate.of(2026, 10, 10))
                .heureDebut(LocalTime.of(14, 0))
                .heureFin(LocalTime.of(16, 0))
                .pointCollecte(point)
                .build();
        Categorie epicerie = Categorie.builder().id(2L).nom("Épicerie").build();
        oeufs = Produit.builder()
                .Id(21L)
                .nom("Œufs bio x6")
                .unite(UniteProduit.BARQUETTE)
                .categorie(epicerie)
                .build();
        lait = Produit.builder()
                .Id(22L)
                .nom("Lait entier 1 L")
                .unite(UniteProduit.LITRE)
                .categorie(epicerie)
                .build();
    }

    @Test
    void generatesReadablePdfWithAggregatedProductsIncidentsAndAccents() throws Exception {
        Commande inPrep = commande(
                101L,
                "CMD-00101",
                "A3F921",
                StatutCommande.IN_PREP,
                true,
                "Œufs abîmés à remplacer",
                "Préparer séparément",
                "Élodie",
                "Façade",
                ligne(oeufs, "2"),
                ligne(lait, "1")
        );
        Commande ready = commande(
                102L,
                "CMD-00102",
                "B7C842",
                StatutCommande.READY,
                false,
                null,
                null,
                "Anaïs",
                "Noël",
                ligne(oeufs, "3.5")
        );
        Commande cancelled = commande(
                103L,
                "CMD-ANNULÉE",
                "CANCEL",
                StatutCommande.CANCELLED,
                false,
                null,
                null,
                "Membre",
                "Annulé",
                ligne(lait, "99")
        );

        PreparationPdfDocument pdf = service.generate(creneau, List.of(inPrep, ready, cancelled));
        String text = extractText(pdf.content());

        assertThat(pdf.content()).isNotEmpty();
        assertThat(new String(pdf.content(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(pdf.fileName())
                .isEqualTo("preparation_Depot_Bruxelles_Centre_2026-10-10_14h00.pdf");
        assertThat(text)
                .contains("TERRA SANA")
                .contains("DOSSIER DE PRÉPARATION")
                .contains("Dépôt Bruxelles Centre")
                .contains("Rue de l'Été 10, Bruxelles")
                .contains("10 octobre 2026")
                .contains("14h00 - 16h00")
                .contains("Nombre de commandes")
                .contains("2")
                .contains("Œufs bio x6")
                .contains("5,5")
                .contains("Lait entier 1 L")
                .contains("1")
                .contains("Commande CMD-00101")
                .contains("Élodie Façade")
                .contains("Code retrait : A3F921")
                .contains("INCIDENT DE PRÉPARATION")
                .contains("Œufs abîmés à remplacer")
                .contains("Préparer séparément")
                .contains("Commande CMD-00102")
                .contains("Code retrait : B7C842")
                .doesNotContain("CMD-ANNULÉE")
                .doesNotContain("CANCEL");
    }

    @Test
    void repeatedGenerationDoesNotModifyOrdersOrLines() {
        LigneCommande line = ligne(oeufs, "4");
        Commande commande = commande(
                201L,
                "CMD-00201",
                "D4E567",
                StatutCommande.IN_PREP,
                false,
                null,
                null,
                "Léa",
                "François",
                line
        );

        PreparationPdfDocument first = service.generate(creneau, List.of(commande));
        PreparationPdfDocument second = service.generate(creneau, List.of(commande));

        assertThat(first.content()).isNotEmpty();
        assertThat(second.content()).isNotEmpty();
        assertThat(commande.getStatut()).isEqualTo(StatutCommande.IN_PREP);
        assertThat(commande.isIncidentPreparation()).isFalse();
        assertThat(line.getQuantite()).isEqualByComparingTo("4");
    }

    @Test
    void twentyOrdersSpanSeveralPagesWithoutLosingDetails() throws Exception {
        List<Commande> commandes = IntStream.rangeClosed(1, 20)
                .mapToObj(index -> commande(
                        300L + index,
                        "CMD-LONG-" + String.format("%02d", index),
                        "CODE" + index,
                        StatutCommande.IN_PREP,
                        false,
                        null,
                        "Commentaire de préparation " + index,
                        "Membre",
                        "Numéro " + index,
                        ligne(oeufs, "1"),
                        ligne(lait, "2")
                ))
                .toList();

        PreparationPdfDocument pdf = service.generate(creneau, commandes);
        PdfReader reader = new PdfReader(pdf.content());
        try {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
        } finally {
            reader.close();
        }
        assertThat(extractText(pdf.content()))
                .contains("Commande CMD-LONG-01")
                .contains("Commande CMD-LONG-20");
    }

    private Commande commande(
            Long id,
            String numero,
            String codeRetrait,
            StatutCommande statut,
            boolean incident,
            String motifIncident,
            String commentaire,
            String prenom,
            String nom,
            LigneCommande... lignes
    ) {
        Commande commande = Commande.builder()
                .id(id)
                .numero(numero)
                .codeRetrait(codeRetrait)
                .statut(statut)
                .incidentPreparation(incident)
                .motifIncidentPreparation(motifIncident)
                .commentaire(commentaire)
                .membre(Membre.builder().prenom(prenom).nom(nom).build())
                .creneauCollecte(creneau)
                .lignes(List.of(lignes))
                .build();
        for (LigneCommande ligne : lignes) {
            ligne.setCommande(commande);
        }
        return commande;
    }

    private LigneCommande ligne(Produit produit, String quantite) {
        return LigneCommande.builder()
                .produit(produit)
                .quantite(new BigDecimal(quantite))
                .build();
    }

    private String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
