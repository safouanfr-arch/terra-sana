package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name="TCATEGORIE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"produits", "promotions"})


public class Categorie {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "Idcategorie")
    private Long id;
	
	@Column(name = "nom", length = 100, nullable = false, unique= true)
    private String nom;

	@Column(name= "description", columnDefinition = "TEXT")
	private String description;
	
	@Builder.Default
	@Column(name="actif", nullable = false)
	private Boolean actif = true; 
	
	@Builder.Default
	@OneToMany(mappedBy = "categorie")
	private List<Produit> produits = new ArrayList<>();
	
	@Builder.Default
	@OneToMany(mappedBy = "categorie")
	private List<PromotionCategorie> promotions = new ArrayList<>();
	
	public void activer() {
		this.actif = true;
	}
	public void desactiver() {
		this.actif = false;
	}
	
	
}
