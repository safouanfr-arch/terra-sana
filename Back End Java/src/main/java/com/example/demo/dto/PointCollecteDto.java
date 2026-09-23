package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointCollecteDto {

    private Long id;

    @NotBlank
    private String nom;

    @NotBlank
    private String adresse;

    private Boolean actif;

    // Liste des créneaux associés (en lecture)
    private List<CreneauCollecteDto> creneaux;
}
