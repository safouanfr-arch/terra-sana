package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.model.enums.TypeMouvementStock;

@Entity
@Table(
        name = "TMOUVEMENT_STOCK",
        indexes = {
                @Index(name = "idx_mvt_produit", columnList = "FKproduit"),
                @Index(name = "idx_mvt_commande", columnList = "FKcommande"),
                @Index(name = "idx_mvt_type", columnList = "type_mouvement"),
                @Index(name = "idx_mvt_date", columnList = "mouvement")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"produit", "commande", "administrateur"})
public class MouvementStock {

    // ---------------------------------------------------------
    // Identifiant STRING (ex: "MVT-20241201-83472")
    // ---------------------------------------------------------
    @Id
    @EqualsAndHashCode.Include
    @Column(name = "Idmouvement", length = 50)
    private String id;

    
    // ---------------------------------------------------------
    // Données de mouvement
    // ---------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false)
    private TypeMouvementStock type;

    @Column(name = "quantite", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantite;

    @Column(name = "stock_avant", precision = 10, scale = 2, nullable = false)
    private BigDecimal stockAvant;

    @Column(name = "stock_apres", precision = 10, scale = 2, nullable = false)
    private BigDecimal stockApres;

    @Builder.Default
    @Column(name = "mouvement")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    
    // ---------------------------------------------------------
    // Relations principales
    // ---------------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "FKproduit", nullable = false)
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "FKcommande")
    private Commande commande;

    @ManyToOne
    @JoinColumn(name = "FKadministrateur")
    private Administrateur administrateur;
    // ---------------------------------------------------------
    // Méthodes métier simples (OK dans l'entité)
    // ---------------------------------------------------------

    /**
     * Retourne vrai si ce mouvement est une sortie de stock.
     */
    public boolean estSortie() {
        return switch (type) {
            case RESERVATION, VENTE -> true;
            default -> false;
        };
    }

    /**
     * Retourne vrai si ce mouvement est une entrée de stock.
     */
    public boolean estEntree() {
        return switch (type) {
            case ANNULATION, AJUSTEMENT -> true;
            default -> false;
        };
    }
}
