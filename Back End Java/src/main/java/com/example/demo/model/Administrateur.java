package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "TADMINISTRATEUR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"demandesTraitees", "mouvementsStockRealises"})
public class Administrateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idadministrateur")
    private Long Id;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100, nullable = false)
    private String prenom;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe", length = 255, nullable = false)
    private String motDePasse;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "derniere_connexion")
    private LocalDateTime dateConnexion;

    @Builder.Default
    @OneToMany(mappedBy = "administrateur")
    private List<DemandeAdhesion> demandesTraitees = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "administrateur")
    private List<MouvementStock> mouvementsStockRealises = new ArrayList<>();

    public void connecter() {
        this.dateConnexion = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
