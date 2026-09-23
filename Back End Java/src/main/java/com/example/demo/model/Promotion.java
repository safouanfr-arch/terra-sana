package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.model.enums.TypeReduction;

@Entity
@Table(name = "TPROMOTION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"produits", "categories"})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idpromotion")
    private Long id;

    @Column(name = "nom", length = 200, nullable = false)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_reduction", nullable = false)
    private TypeReduction typeReduction;

    @Column(name = "valeur_reduction", precision = 10, scale = 2, nullable = false)
    private BigDecimal valeurReduction;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Builder.Default
    @Column(name = "actif")
    private Boolean actif = true;

    // -----------------------
    // Relations N:M via tables de jointure
    // -----------------------

    @Builder.Default
    @OneToMany(mappedBy = "promotion")
    private List<PromotionProduit> produits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "promotion")
    private List<PromotionCategorie> categories = new ArrayList<>();
    
    
    public void activer() {
		this.actif = true;
	}
	public void desactiver() {
		this.actif = false;
	}

}
