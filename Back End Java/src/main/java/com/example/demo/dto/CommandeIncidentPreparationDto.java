package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CommandeIncidentPreparationDto {

    @NotBlank(message = "Le motif de l'incident est obligatoire.")
    @Size(max = 500, message = "Le motif de l'incident ne peut pas depasser 500 caracteres.")
    private String motif;
}
