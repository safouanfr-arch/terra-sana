package com.example.demo.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name="TPROMOTION_CATEGORIE",
	   uniqueConstraints = @UniqueConstraint(name = "uniq_categorie_promotion", //génère une contrainte d’unicité SQL sur la table.
	    									 columnNames = {"FKcategorie", "FKpromotion"}))

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"categorie", "promotion"})


public class PromotionCategorie {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
    @Column(name = "Idpromotion_categorie")
	private Long id;

	
	@ManyToOne
	@JoinColumn(name = "FKcategorie")
	private Categorie categorie;
	
	@ManyToOne
	@JoinColumn(name = "FKpromotion")
	private Promotion promotion;
	
}
