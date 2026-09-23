package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "TMEMBRE",
        indexes = {
                @Index(name = "idx_membre_email", columnList = "email"),
                @Index(name = "idx_membre_actif", columnList = "actif")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"commandes"})
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idmembre")
    private Long id;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100, nullable = false)
    private String prenom;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "telephone", length = 20, nullable = false)
    private String telephone;

    @Column(name = "adresse", length = 255, nullable = false)
    private String adresse;

    @Column(name = "ville", length = 100, nullable = false)
    private String ville;

    @Column(name = "code_postal", length = 10, nullable = false)
    private String codePostal;

    @Builder.Default
    @Column(name = "pays", length = 100, nullable = false)
    private String pays = "Belgique";

    @Column(name = "mot_de_passe", length = 255, nullable = false)
    private String motDePasse;

    @Builder.Default
    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "token_mot_de_passe", length = 120)
    private String tokenMotDePasse;

    @Column(name = "expiration_token_mot_de_passe")
    private LocalDateTime dateExpirationTokenMotDePasse;

    @Column(name = "creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "login")
    private LocalDateTime dateLogin;

    @Builder.Default
    @OneToMany(mappedBy = "membre")
    private List<Commande> commandes = new ArrayList<>();

    public void activer() {
        this.actif = true;
    }

    public void desactiver() {
        this.actif = false;
    }

    public void definirTokenMotDePasse(String token, LocalDateTime expiration) {
        this.tokenMotDePasse = token;
        this.dateExpirationTokenMotDePasse = expiration;
    }

    public void effacerTokenMotDePasse() {
        this.tokenMotDePasse = null;
        this.dateExpirationTokenMotDePasse = null;
    }

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateLogin = LocalDateTime.now();
    }
}
