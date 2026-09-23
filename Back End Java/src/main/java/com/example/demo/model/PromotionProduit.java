package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name="TPROMOTION_PRODUIT",
	   uniqueConstraints = @UniqueConstraint(name = "uniq_produit_promotion", //génère une contrainte d’unicité SQL sur la table.
	    									 columnNames = {"FKproduit", "FKpromotion"}))

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"produit", "promotion"})


public class PromotionProduit {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
    @Column(name = "Idpromotion_produit")
	private Long id;

	
	@ManyToOne
	@JoinColumn(name = "FKproduit")
	private Produit produit;
	
	@ManyToOne
	@JoinColumn(name = "FKpromotion")
	private Promotion promotion;
	
}