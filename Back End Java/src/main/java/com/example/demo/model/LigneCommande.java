package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "TLIGNE_COMMANDE",
        indexes = {
                @Index(name = "idx_ligne_commande_commande", columnList = "FKcommande"),
                @Index(name = "idx_ligne_commande_produit", columnList = "FKproduit")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"commande", "produit"})
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idligne_commande")
    private Long id;

    // --------- Champs métier ---------

    @Column(name = "quantite", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire", precision = 10, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(name = "prix_unitaire_original", precision = 10, scale = 2)
    private BigDecimal prixUnitaireOriginal;

    @Column(name = "reduction_unitaire", precision = 10, scale = 2)
    private BigDecimal reductionUnitaire;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "promotion_nom", length = 200)
    private String promotionNom;

    @Column(name = "taux_tva", precision = 5, scale = 2, nullable = false)
    private BigDecimal tauxTVA;

    @Column(name = "montant_ligne_ht", precision = 10, scale = 2, nullable = false)
    private BigDecimal montantHT;

    @Column(name = "montant_ligne_ttc", precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTTC;
    
// --------- Relations ---------

    @ManyToOne
    @JoinColumn(name = "FKcommande", nullable = false)
    private Commande commande;

    @ManyToOne
    @JoinColumn(name = "FKproduit", nullable = false)
    private Produit produit;
   
}
