package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreparationGroupFinalizationDto {

    private Long creneauId;
    private int commandesFinalisees;
    private int commandesExcluesIncident;
    private int commandesIgnoreesNonEligibles;
}
