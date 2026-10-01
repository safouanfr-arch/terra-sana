package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.enums.EmailSendStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparationEmailStatusDto {

    private Long creneauId;
    private EmailSendStatus statutEnvoi;
    private LocalDateTime dateTentativeEnvoi;
    private LocalDateTime dateEnvoi;
    private String erreurEnvoi;
    private boolean envoiConfigure;
}
