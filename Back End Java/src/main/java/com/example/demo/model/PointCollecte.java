package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "TPOINT_COLLECTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"creneaux"})
public class PointCollecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idpoint_collecte")
    private Long id;

    @Column(name = "nom", length = 200, nullable = false)
    private String nom;

    @Column(name = "adresse", length = 255, nullable = false)
    private String adresse;

    @Builder.Default
    @Column(name = "actif")
    private Boolean actif = true;

    // -----------------------
    // Relation inverse
    // -----------------------

    @Builder.Default
    @OneToMany(mappedBy = "pointCollecte")
    @OrderBy("creneau ASC, heureDebut ASC")
    private List<CreneauCollecte> creneaux = new ArrayList<>();


    // -----------------------
    // Méthodes métier simples
    // -----------------------

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }
}
