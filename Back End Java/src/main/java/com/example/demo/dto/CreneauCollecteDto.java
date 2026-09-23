package com.example.demo.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreneauCollecteDto {

    private Long id;

    @NotNull
    private Long pointCollecteId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime heureDebut;

    @NotNull
    private LocalTime heureFin;

    private Boolean actif;
}
