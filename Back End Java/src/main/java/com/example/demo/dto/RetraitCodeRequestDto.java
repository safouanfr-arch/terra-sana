package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RetraitCodeRequestDto {

    @NotBlank(message = "Le code de retrait est obligatoire.")
    @Size(max = 20, message = "Le code de retrait ne peut pas depasser 20 caracteres.")
    private String codeRetrait;
}
