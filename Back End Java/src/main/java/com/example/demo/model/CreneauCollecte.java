package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
		name = "TCRENEAU_COLLECTE",
		uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_creneau_point_date_horaire",
            columnNames = {"FKpoint_collecte", "creneau", "heure_debut", "heure_fin"}
        )
    },
        indexes = {
                @Index(name = "idx_creneau_point_collecte", columnList = "FKpoint_collecte"),
                @Index(name = "idx_creneau", columnList = "creneau"),
                @Index(name = "idx_creneau_point_date", columnList = "FKpoint_collecte,creneau")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"pointCollecte", "commandes"})
public class CreneauCollecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idcreneau")
    private Long id;

    @Column(name = "creneau", nullable = false)
    private LocalDate creneau;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Builder.Default
    @Column(name = "actif")
    private Boolean actif = true;

    // ---------------------
    // Relations inverses
    // ---------------------
    @ManyToOne
    @JoinColumn(name = "FKpoint_collecte", nullable = false)
    private PointCollecte pointCollecte;
    
    @Builder.Default
    @OneToMany(mappedBy = "creneauCollecte")
    private List<Commande> commandes = new ArrayList<>();

    // ---------------------
    // Méthodes métier simples
    // ---------------------
    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

}
