package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.model.enums.StatutCommande;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "TCOMMANDE",
        indexes = {
                @Index(name = "idx_commande_membre", columnList = "FKmembre"),
                @Index(name = "idx_commande_statut", columnList = "statut"),
                @Index(name = "idx_commande_paye", columnList = "paye"),
                @Index(name = "idx_commande_date_commande", columnList = "commande"),
                @Index(name = "idx_commande_creneau", columnList = "FKcreneau_collecte")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"membre", "creneauCollecte", "lignes", "mouvementsStock"})
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idcommande")
    private Long id;

    @Column(name = "numero", length = 50, nullable = false, unique = true)
    private String numero;

    @Column(name = "code_retrait", length = 20)
    private String codeRetrait;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    @Builder.Default
    private StatutCommande statut = StatutCommande.CONFIRMED;

    @Column(name = "montant_ht", precision = 10, scale = 2, nullable = false)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTVA;

    @Column(name = "montant_ttc", precision = 10, scale = 2, nullable = false)
    private BigDecimal montantTTC;

    @Builder.Default
    @Column(name = "paye")
    private Boolean paye = false;

    @Column(name = "paiement")
    private LocalDateTime datePaiement;

    @Builder.Default
    @Column(name = "commande")
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "retrait_prevu", nullable = false)
    private LocalDateTime dateRetraitPrevu;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "annulation")
    private LocalDateTime dateAnnulation;

    @ManyToOne
    @JoinColumn(name = "FKmembre", nullable = false)
    private Membre membre;

    @ManyToOne
    @JoinColumn(name = "FKcreneau_collecte", nullable = false)
    private CreneauCollecte creneauCollecte;

    @Builder.Default
    @OneToMany(mappedBy = "commande")
    private List<LigneCommande> lignes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "commande")
    private List<MouvementStock> mouvementsStock = new ArrayList<>();

    public PointCollecte getPointCollecte() {
        return creneauCollecte != null ? creneauCollecte.getPointCollecte() : null;
    }
}
