package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDto {

    private Long id;

    @NotBlank
    private String nom;

    private String description;

    @NotBlank
    private String typeReduction;

    @NotNull
    private BigDecimal valeurReduction;

    @NotNull
    private LocalDate dateDebut;

    @NotNull
    private LocalDate dateFin;

    private Boolean actif;
    private List<Long> produitIds;
    private List<Long> categorieIds;
}
