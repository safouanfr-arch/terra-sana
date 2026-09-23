package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

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
public class CommandeCreneauResumeDto {

    private Long id;
    private Long pointCollecteId;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String horaire;
}
