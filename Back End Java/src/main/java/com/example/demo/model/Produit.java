package com.example.demo.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.model.enums.TagAlimentaire;
import com.example.demo.model.enums.UniteProduit;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "TPRODUIT",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"nom", "unite", "FKcategorie"})
        },
        indexes = {
                @Index(name = "idx_produit_categorie", columnList = "FKcategorie"),
                @Index(name = "idx_produit_actif", columnList = "actif")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"categorie", "lignesCommandes", "mouvementsStock", "promotions"})
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idproduit")
    private Long Id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "nom", length = 200, nullable = false)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "prix_unitaire", precision = 10, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "unite", nullable = false)
    private UniteProduit unite;

    @Builder.Default
    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTva = BigDecimal.valueOf(6.00);

    @Builder.Default
    @Column(name = "stock_actuel", precision = 10, scale = 2)
    private BigDecimal stockActuel = BigDecimal.ZERO;

    @Lob
    @Column(name = "images", columnDefinition = "CLOB")
    private String images;

    @Builder.Default
    @Column(name = "actif")
    private Boolean actif = true;

    @Builder.Default
    @ElementCollection(targetClass = TagAlimentaire.class)
    @CollectionTable(name = "TPRODUIT_TAGS", joinColumns = @JoinColumn(name = "FKproduit"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 50)
    private Set<TagAlimentaire> tagsAlimentaires = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "FKcategorie", nullable = false)
    private Categorie categorie;

    @Builder.Default
    @OneToMany(mappedBy = "produit")
    private List<LigneCommande> lignesCommandes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "produit")
    private List<MouvementStock> mouvementsStock = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "produit")
    private List<PromotionProduit> promotions = new ArrayList<>();

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }
}
