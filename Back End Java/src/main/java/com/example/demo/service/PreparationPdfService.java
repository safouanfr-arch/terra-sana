package com.example.demo.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PreparationPdfDocument;
import com.example.demo.model.Commande;
import com.example.demo.model.CreneauCollecte;
import com.example.demo.model.LigneCommande;
import com.example.demo.model.Produit;
import com.example.demo.model.enums.StatutCommande;
import com.example.demo.model.enums.UniteProduit;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

@Service
public class PreparationPdfService {

    private static final Color PRIMARY = new Color(47, 93, 80);
    private static final Color BORDER = new Color(177, 190, 184);
    private static final Color TEXT = new Color(32, 42, 38);
    private static final Color INCIDENT_BACKGROUND = new Color(255, 244, 204);
    private static final Color INCIDENT_TEXT = new Color(132, 78, 0);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.FRENCH);
    private static final DateTimeFormatter GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH'h'mm");

    private Clock clock = Clock.systemDefaultZone();

    public PreparationPdfDocument generate(CreneauCollecte creneau, List<Commande> commandes) {
        List<Commande> includedCommandes = commandes.stream()
                .filter(this::isIncluded)
                .sorted(Comparator.comparing(Commande::getNumero, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 38, 38, 42, 42);
            PdfWriter.getInstance(document, output);
            document.addTitle("Terra Sana - Dossier de préparation");
            document.addSubject("Préparation du créneau de collecte");
            document.addCreator("Terra Sana");
            document.open();

            Fonts fonts = createFonts();
            addHeader(document, fonts, creneau, includedCommandes);
            addPickList(document, fonts, includedCommandes);
            addOrderDetails(document, fonts, includedCommandes);

            document.close();
            return new PreparationPdfDocument(buildFileName(creneau), output.toByteArray());
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Impossible de générer le dossier PDF de préparation.", exception);
        }
    }

    private void addHeader(
            Document document,
            Fonts fonts,
            CreneauCollecte creneau,
            List<Commande> commandes
    ) throws DocumentException {
        Paragraph brand = new Paragraph("TERRA SANA", fonts.brand());
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(3);
        document.add(brand);

        Paragraph title = new Paragraph("DOSSIER DE PRÉPARATION", fonts.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(18);
        document.add(title);

        PdfPTable metadata = new PdfPTable(new float[] { 31, 69 });
        metadata.setWidthPercentage(100);
        metadata.setSpacingAfter(14);
        addMetadataRow(metadata, fonts, "Point de collecte", value(creneau.getPointCollecte().getNom()));
        addMetadataRow(metadata, fonts, "Adresse", value(creneau.getPointCollecte().getAdresse()));
        addMetadataRow(metadata, fonts, "Date", creneau.getCreneau().format(DATE_FORMAT));
        addMetadataRow(
                metadata,
                fonts,
                "Créneau",
                creneau.getHeureDebut().format(TIME_FORMAT) + " - " + creneau.getHeureFin().format(TIME_FORMAT)
        );
        addMetadataRow(metadata, fonts, "Nombre de commandes", String.valueOf(commandes.size()));
        addMetadataRow(metadata, fonts, "Généré le", LocalDateTime.now(clock).format(GENERATED_FORMAT));
        document.add(metadata);

        long incidentCount = commandes.stream().filter(Commande::isIncidentPreparation).count();
        if (incidentCount > 0) {
            PdfPCell alertCell = new PdfPCell(new Phrase(
                    "ATTENTION : " + incidentCount + " commande(s) avec incident de préparation",
                    fonts.incidentBold()
            ));
            alertCell.setBackgroundColor(INCIDENT_BACKGROUND);
            alertCell.setBorderColor(new Color(226, 183, 92));
            alertCell.setPadding(9);

            PdfPTable alert = new PdfPTable(1);
            alert.setWidthPercentage(100);
            alert.setSpacingAfter(16);
            alert.addCell(alertCell);
            document.add(alert);
        }
    }

    private void addPickList(Document document, Fonts fonts, List<Commande> commandes)
            throws DocumentException {
        Paragraph heading = sectionHeading("1. PICK-LIST GLOBALE", fonts);
        document.add(heading);

        List<PickListItem> items = aggregateProducts(commandes);
        if (items.isEmpty()) {
            Paragraph empty = new Paragraph("Aucun produit à préparer pour ce créneau.", fonts.body());
            empty.setSpacingAfter(16);
            document.add(empty);
            return;
        }

        PdfPTable table = new PdfPTable(new float[] { 7, 57, 20, 16 });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);
        table.setSpacingAfter(20);
        addTableHeader(table, fonts, "", "Produit", "Quantité totale", "Unité");

        for (PickListItem item : items) {
            table.addCell(checkBoxCell());
            table.addCell(bodyCell(item.productName(), fonts));
            table.addCell(centeredCell(formatQuantity(item.quantity()), fonts));
            table.addCell(centeredCell(item.unit(), fonts));
        }
        document.add(table);
    }

    private void addOrderDetails(Document document, Fonts fonts, List<Commande> commandes)
            throws DocumentException {
        document.add(sectionHeading("2. DÉTAIL DES COMMANDES", fonts));

        if (commandes.isEmpty()) {
            document.add(new Paragraph("Aucune commande en préparation ou prête pour ce créneau.", fonts.body()));
            return;
        }

        for (Commande commande : commandes) {
            Paragraph orderTitle = new Paragraph(
                    "Commande " + value(commande.getNumero()),
                    fonts.orderTitle()
            );
            orderTitle.setSpacingBefore(6);
            orderTitle.setSpacingAfter(6);
            document.add(orderTitle);

            PdfPTable metadata = new PdfPTable(new float[] { 50, 50 });
            metadata.setWidthPercentage(100);
            metadata.setSpacingAfter(7);
            metadata.addCell(detailCell(
                    "Membre : " + memberName(commande),
                    fonts
            ));
            metadata.addCell(detailCell(
                    "Code retrait : " + value(commande.getCodeRetrait()),
                    fonts
            ));
            document.add(metadata);

            if (commande.isIncidentPreparation()) {
                String motif = hasText(commande.getMotifIncidentPreparation())
                        ? commande.getMotifIncidentPreparation().trim()
                        : "Motif non renseigné";
                PdfPCell incidentCell = new PdfPCell(new Phrase(
                        "INCIDENT DE PRÉPARATION - " + motif,
                        fonts.incidentBold()
                ));
                incidentCell.setBackgroundColor(INCIDENT_BACKGROUND);
                incidentCell.setBorderColor(new Color(226, 183, 92));
                incidentCell.setPadding(7);
                PdfPTable incident = new PdfPTable(1);
                incident.setWidthPercentage(100);
                incident.setSpacingAfter(7);
                incident.addCell(incidentCell);
                document.add(incident);
            }

            PdfPTable products = new PdfPTable(new float[] { 8, 58, 18, 16 });
            products.setWidthPercentage(100);
            products.setHeaderRows(1);
            products.setSplitRows(true);
            products.setSplitLate(false);
            addTableHeader(products, fonts, "", "Produit", "Quantité", "Unité");
            for (LigneCommande ligne : sortedLines(commande.getLignes())) {
                Produit produit = ligne.getProduit();
                products.addCell(checkBoxCell());
                products.addCell(bodyCell(produit != null ? value(produit.getNom()) : "Produit", fonts));
                products.addCell(centeredCell(formatQuantity(ligne.getQuantite()), fonts));
                products.addCell(centeredCell(productUnit(produit), fonts));
            }
            document.add(products);

            if (hasText(commande.getCommentaire())) {
                Paragraph comment = new Paragraph("Commentaire : " + commande.getCommentaire().trim(), fonts.body());
                comment.setSpacingBefore(6);
                document.add(comment);
            }

            LineSeparator separator = new LineSeparator(0.7f, 100, BORDER, Element.ALIGN_CENTER, -2);
            Paragraph divider = new Paragraph();
            divider.setSpacingBefore(9);
            divider.setSpacingAfter(9);
            divider.add(separator);
            document.add(divider);
        }
    }

    private List<PickListItem> aggregateProducts(List<Commande> commandes) {
        Map<Long, PickListItem> products = new LinkedHashMap<>();
        for (Commande commande : commandes) {
            for (LigneCommande ligne : commande.getLignes()) {
                Produit produit = ligne.getProduit();
                if (produit == null || produit.getId() == null) {
                    continue;
                }
                PickListItem current = products.get(produit.getId());
                BigDecimal quantity = ligne.getQuantite() != null ? ligne.getQuantite() : BigDecimal.ZERO;
                if (current == null) {
                    products.put(produit.getId(), new PickListItem(
                            produit.getId(),
                            produit.getCategorie() != null ? value(produit.getCategorie().getNom()) : "",
                            value(produit.getNom()),
                            quantity,
                            productUnit(produit)
                    ));
                } else {
                    products.put(produit.getId(), current.withQuantity(current.quantity().add(quantity)));
                }
            }
        }

        return products.values().stream()
                .sorted(Comparator.comparing(PickListItem::category, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PickListItem::productName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<LigneCommande> sortedLines(List<LigneCommande> lignes) {
        if (lignes == null) {
            return List.of();
        }
        List<LigneCommande> sorted = new ArrayList<>(lignes);
        sorted.sort(Comparator.comparing(
                ligne -> ligne.getProduit() != null ? value(ligne.getProduit().getNom()) : "",
                String.CASE_INSENSITIVE_ORDER
        ));
        return sorted;
    }

    private boolean isIncluded(Commande commande) {
        return commande.getStatut() == StatutCommande.IN_PREP
                || commande.getStatut() == StatutCommande.READY;
    }

    private void addMetadataRow(PdfPTable table, Fonts fonts, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fonts.label()));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, fonts.body()));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private Paragraph sectionHeading(String text, Fonts fonts) {
        Paragraph heading = new Paragraph(text, fonts.sectionTitle());
        heading.setSpacingBefore(3);
        heading.setSpacingAfter(8);
        return heading;
    }

    private void addTableHeader(PdfPTable table, Fonts fonts, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, fonts.tableHeader()));
            cell.setBackgroundColor(PRIMARY);
            cell.setBorderColor(PRIMARY);
            cell.setPadding(7);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
    }

    private PdfPCell checkBoxCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setMinimumHeight(22);
        cell.setCellEvent(new CheckBoxCellEvent());
        return cell;
    }

    private PdfPCell bodyCell(String text, Fonts fonts) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.body()));
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell centeredCell(String text, Fonts fonts) {
        PdfPCell cell = bodyCell(text, fonts);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell detailCell(String text, Fonts fonts) {
        PdfPCell cell = new PdfPCell(new Phrase(text, fonts.body()));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private String memberName(Commande commande) {
        if (commande.getMembre() == null) {
            return "Membre";
        }
        return (value(commande.getMembre().getPrenom()) + " " + value(commande.getMembre().getNom())).trim();
    }

    private String productUnit(Produit produit) {
        UniteProduit unit = produit != null ? produit.getUnite() : null;
        if (unit == null) {
            return "";
        }
        return switch (unit) {
            case KG -> "kg";
            case PIECE -> "pièce";
            case BARQUETTE -> "barquette";
            case LITRE -> "litre";
        };
    }

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return "0";
        }
        return quantity.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private String buildFileName(CreneauCollecte creneau) {
        String point = sanitizeFilePart(creneau.getPointCollecte().getNom());
        String date = creneau.getCreneau().toString();
        String time = creneau.getHeureDebut().format(TIME_FORMAT);
        return "preparation_" + point + "_" + date + "_" + time + ".pdf";
    }

    private String sanitizeFilePart(String value) {
        String normalized = Normalizer.normalize(value(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9_-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? "point_collecte" : normalized;
    }

    private Fonts createFonts() throws IOException, DocumentException {
        BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        return new Fonts(
                new Font(base, 15, Font.BOLD, PRIMARY),
                new Font(base, 18, Font.BOLD, TEXT),
                new Font(base, 11, Font.BOLD, PRIMARY),
                new Font(base, 9, Font.NORMAL, TEXT),
                new Font(base, 9, Font.BOLD, TEXT),
                new Font(base, 9, Font.BOLD, Color.WHITE),
                new Font(base, 10, Font.BOLD, TEXT),
                new Font(base, 9, Font.BOLD, INCIDENT_TEXT)
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    private record Fonts(
            Font brand,
            Font title,
            Font sectionTitle,
            Font body,
            Font label,
            Font tableHeader,
            Font orderTitle,
            Font incidentBold
    ) {
    }

    private record PickListItem(
            Long productId,
            String category,
            String productName,
            BigDecimal quantity,
            String unit
    ) {
        private PickListItem withQuantity(BigDecimal newQuantity) {
            return new PickListItem(productId, category, productName, newQuantity, unit);
        }
    }

    private static final class CheckBoxCellEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            float size = 8;
            float x = position.getLeft() + (position.getWidth() - size) / 2;
            float y = position.getBottom() + (position.getHeight() - size) / 2;
            canvas.saveState();
            canvas.setColorStroke(PRIMARY);
            canvas.setLineWidth(0.8f);
            canvas.rectangle(x, y, size, size);
            canvas.stroke();
            canvas.restoreState();
        }
    }
}
