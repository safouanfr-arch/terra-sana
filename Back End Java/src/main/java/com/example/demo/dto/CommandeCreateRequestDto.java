package com.example.demo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class CommandeCreateRequestDto {

    private Long membreId;

    @NotNull
    private Long creneauCollecteId;

    private String commentaire;

    @Valid
    @NotEmpty
    private List<CommandeCreateItemDto> produits;
}
