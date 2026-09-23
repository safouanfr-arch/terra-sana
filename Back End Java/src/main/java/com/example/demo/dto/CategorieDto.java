package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieDto {
    private Long id;          // présent dans les réponses, ignoré à la création
    @NotBlank 
    private String nom;
    private String description;
    private Boolean actif;
    private Long compteur; 
}