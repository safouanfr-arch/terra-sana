package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.enums.EmailSendStatus;
import com.example.demo.model.enums.EmailNotificationType;

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
public class GeneratedEmailDto {

    private Long id;
    private EmailNotificationType type;
    private String destinataire;
    private String sujet;
    private String contenuHtml;
    private LocalDateTime createdAt;
    private EmailSendStatus statutEnvoi;
    private LocalDateTime dateTentativeEnvoi;
    private LocalDateTime dateEnvoi;
    private String erreurEnvoi;
    private Long commandeId;
    private Long membreId;
    private Long produitId;
    private Long demandeAdhesionId;
}
